import { expect, test } from '@playwright/test'
import { createIssue, createOrganization, createProject, newUser, signIn, signUp, unique } from './helpers'

test('a new user creates a project, adds an issue and moves it across the board', async ({ page }) => {
  const owner = newUser('owner')
  await signUp(page, owner)
  const organization = unique('E2E Studio')
  await createOrganization(page, organization)
  await createProject(page, 'Launch site')

  await expect(page.getByRole('heading', { name: 'Launch site' })).toBeVisible()
  await createIssue(page, 'Write landing copy')

  const todo = page.getByRole('region', { name: 'To do' })
  const inProgress = page.getByRole('region', { name: 'In progress' })
  const ticket = page.locator('.ticket', { hasText: 'Write landing copy' })
  await expect(todo.locator('.ticket', { hasText: 'Write landing copy' })).toBeVisible()

  await ticket.dragTo(inProgress)

  await expect(inProgress.locator('.ticket', { hasText: 'Write landing copy' })).toBeVisible()
  await expect(todo.locator('.ticket', { hasText: 'Write landing copy' })).toHaveCount(0)

  await page.reload()
  await expect(page.getByRole('region', { name: 'In progress' }).locator('.ticket', { hasText: 'Write landing copy' })).toBeVisible()

  await page.getByRole('button', { name: 'Sign out' }).click()
  await signIn(page, owner)
  await expect(page.getByText(organization).first()).toBeVisible()
})

test('an issue link opens the issue directly', async ({ page }) => {
  await signUp(page, newUser('linker'))
  await createOrganization(page, unique('Link Labs'))
  await createProject(page, 'Deep links')
  await createIssue(page, 'Shareable issue')

  await page.locator('.ticket', { hasText: 'Shareable issue' }).click()
  await expect(page).toHaveURL(/[?&]issue=/)
  const link = page.url()

  await page.goto('/')
  await page.goto(link)
  await expect(page.getByRole('dialog').getByText('Shareable issue').first()).toBeVisible()
})

test('the chosen theme survives a reload', async ({ page }) => {
  await signUp(page, newUser('themer'))
  await page.getByRole('button', { name: 'Switch to dark theme' }).click()
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')
  await page.reload()
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')
  await page.getByRole('button', { name: 'Switch to light theme' }).click()
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'light')
})
