import { expect, type Page } from '@playwright/test'

export type TestUser = { username: string; email: string; password: string }

export function unique(name: string) {
  return `${name} ${Date.now().toString(36)}${Math.random().toString(36).slice(2, 5)}`
}

export function newUser(prefix: string): TestUser {
  const id = `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 6)}`
  return { username: `${prefix}_${id}`, email: `${prefix}.${id}@e2e.test`, password: `E2e-${id}-pass` }
}

export async function signUp(page: Page, user: TestUser) {
  await page.goto('/')
  await page.getByRole('button', { name: 'Create an account' }).click()
  await page.getByLabel('Username').fill(user.username)
  await page.getByLabel('Email').fill(user.email)
  await page.getByLabel('Password').fill(user.password)
  await page.getByRole('button', { name: 'Sign up' }).click()
  await expect(page.getByRole('button', { name: 'Sign out' })).toBeVisible()
}

export async function signIn(page: Page, user: TestUser) {
  await page.goto('/')
  await page.getByLabel('Email').fill(user.email)
  await page.getByLabel('Password').fill(user.password)
  await page.getByRole('button', { name: 'Sign in' }).click()
  await expect(page.getByRole('button', { name: 'Sign out' })).toBeVisible()
}

export async function createOrganization(page: Page, title: string) {
  await page.getByRole('button', { name: 'Create organization' }).first().click()
  const dialog = page.getByRole('dialog', { name: 'Create organization' })
  await dialog.getByLabel('Title').fill(title)
  await dialog.getByRole('button', { name: 'Create' }).click()
  await expect(dialog).toBeHidden()
  const navigation = page.getByRole('navigation', { name: 'Main navigation' })
  await expect(navigation.getByText(title, { exact: true })).toBeVisible()
  await navigation.getByText('Projects', { exact: true }).click()
  await expect(page.getByRole('button', { name: 'Create project' }).first()).toBeVisible()
}

export async function createProject(page: Page, title: string) {
  await page.getByRole('button', { name: 'Create project' }).first().click()
  const dialog = page.getByRole('dialog', { name: 'Create project' })
  await dialog.getByLabel('Title').fill(title)
  await dialog.getByRole('button', { name: 'Create' }).click()
  await expect(dialog).toBeHidden()
}

export async function createIssue(page: Page, title: string) {
  await page.getByRole('button', { name: 'Create issue' }).first().click()
  const dialog = page.getByRole('dialog', { name: 'Create issue' })
  await dialog.getByLabel('Title').fill(title)
  await dialog.getByRole('button', { name: 'Create issue' }).click()
  await expect(dialog).toBeHidden()
  const detail = page.getByRole('dialog', { name: title })
  await expect(detail).toBeVisible()
  await page.keyboard.press('Escape')
  await expect(detail).toBeHidden()
}
