import { beforeEach, describe, expect, it, vi } from 'vitest'

const sessionKey = 'tasktracker.session'

function json(status: number, body: unknown) {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

async function loadApi(session = { accessToken: 'old-access', refreshToken: 'old-refresh' }) {
  sessionStorage.setItem(sessionKey, JSON.stringify(session))
  vi.resetModules()
  return import('./api')
}

describe('api client', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('sends the access token and returns the body', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(json(200, { id: 'u1', email: 'a@b.c', username: 'alice' }))
    const { getProfile } = await loadApi()

    await expect(getProfile()).resolves.toMatchObject({ username: 'alice' })
    const [url, init] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/auth/me')
    expect(new Headers(init?.headers).get('Authorization')).toBe('Bearer old-access')
  })

  it('refreshes once for parallel 401 responses and retries with the new token', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const url = String(input)
      const token = new Headers(init?.headers).get('Authorization')
      if (url === '/api/auth/refresh') return json(200, { accessToken: 'new-access', refreshToken: 'new-refresh' })
      return token === 'Bearer new-access' ? json(200, { id: 'u1', email: 'a@b.c', username: 'alice' }) : json(401, {})
    })
    const { getProfile } = await loadApi()

    const results = await Promise.all([getProfile(), getProfile(), getProfile()])

    expect(results.every(user => user.username === 'alice')).toBe(true)
    expect(fetchMock.mock.calls.filter(([url]) => String(url) === '/api/auth/refresh')).toHaveLength(1)
    expect(JSON.parse(sessionStorage.getItem(sessionKey)!)).toEqual({ accessToken: 'new-access', refreshToken: 'new-refresh' })
  })

  it('signs the user out when the refresh token is rejected', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(json(401, {}))
    const { getProfile, hasSession } = await loadApi()

    await expect(getProfile()).rejects.toMatchObject({ status: 401 })
    expect(hasSession()).toBe(false)
    expect(sessionStorage.getItem(sessionKey)).toBeNull()
  })

  it('turns raw access and not-found messages into readable text', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch')
    const { api } = await loadApi()

    fetchMock.mockResolvedValueOnce(json(403, { detail: 'Access denied to project 1234' }))
    await expect(api.organization('org')).rejects.toThrow(
      'You are not a member of this project. Ask an organization owner or admin to add you.',
    )

    fetchMock.mockResolvedValueOnce(json(404, { detail: 'Issue with ID 1234 not found' }))
    await expect(api.organization('org')).rejects.toThrow('This issue does not exist or was deleted.')

    fetchMock.mockResolvedValueOnce(json(403, { detail: 'Only OWNER can transfer ownership' }))
    await expect(api.organization('org')).rejects.toThrow('Only OWNER can transfer ownership')
  })

  it('loads every page for listAll endpoints', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(async input => {
      const page = Number(new URL(String(input), 'http://localhost').searchParams.get('page'))
      return json(200, { content: [{ id: `p${page}` }], page, size: 100, totalPages: 3, totalElements: 3 })
    })
    const { api } = await loadApi()

    await expect(api.allProjects('org')).resolves.toEqual([{ id: 'p0' }, { id: 'p1' }, { id: 'p2' }])
    expect(fetchMock).toHaveBeenCalledTimes(3)
  })
})
