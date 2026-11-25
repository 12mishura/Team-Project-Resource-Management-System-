import { expect, test } from '@playwright/test'
import { createIssue, createOrganization, createProject, newUser, signUp, unique } from './helpers'

test('a member logs time on an issue and sees it in the timesheet and the workload', async ({ page }) => {
  const owner = newUser('timer')
  await signUp(page, owner)
  await createOrganization(page, unique('Time Studio'))
  await createProject(page, 'Billing')

  const sections = page.getByRole('navigation', { name: 'Project sections' })
  await sections.getByRole('button', { name: 'Members' }).click()
  await page.getByRole('combobox', { name: 'Organization member' }).click()
  await page.getByRole('option', { name: new RegExp(owner.username) }).click()
  await page.getByRole('button', { name: 'Add', exact: true }).click()
  await expect(page.getByRole('combobox', { name: `Role of ${owner.username}` })).toBeVisible()

  await sections.getByRole('button', { name: 'Board' }).click()
  await createIssue(page, 'Invoice export')
  await page.locator('.ticket', { hasText: 'Invoice export' }).click()

  const detail = page.getByRole('dialog', { name: 'Invoice export' })
  await detail.getByRole('button', { name: 'Edit' }).click()
  const editor = page.getByRole('dialog', { name: 'Edit issue' })
  await editor.getByLabel('Estimate').fill('4h')
  await editor.getByRole('button', { name: 'Save changes' }).click()

  const time = detail.getByRole('region', { name: 'Time tracking' })
  await expect(time).toContainText('Estimate4h')
  await time.getByRole('button', { name: 'Log time' }).click()
  const logDialog = page.getByRole('dialog', { name: 'Log time' })
  await logDialog.getByLabel('Time spent').fill('1h 30m')
  await logDialog.getByLabel('What did you work on?').fill('CSV layout')
  await logDialog.getByRole('button', { name: 'Log time' }).click()
  await expect(logDialog).toBeHidden()

  await expect(time).toContainText('Logged1h 30m')
  await expect(time).toContainText('Remaining2h 30m')
  await detail.getByRole('button', { name: 'Work log' }).click()
  await expect(detail.getByText('CSV layout')).toBeVisible()
  await page.keyboard.press('Escape')

  await expect(page.locator('.ticket', { hasText: 'Invoice export' }).locator('.time-chip')).toHaveText('1h 30m / 4h')

  await sections.getByRole('button', { name: 'Workload' }).click()
  const row = page.getByRole('row', { name: new RegExp(owner.username) })
  await expect(row).toContainText('1.5h / 40h')

  await page.getByRole('navigation', { name: 'Main navigation' }).getByText('Timesheet', { exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Timesheet' })).toBeVisible()
  await expect(page.getByRole('row', { name: /Invoice export/ })).toContainText('1h 30m')
})
