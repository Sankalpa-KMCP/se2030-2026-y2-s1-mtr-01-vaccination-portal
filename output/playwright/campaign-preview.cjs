async (page) => {
  const user = { id: 1, fullName: 'Preview Administrator', email: 'preview@example.test', roles: ['ROLE_ADMIN'] };
  const campaigns = ['ACTIVE', 'DRAFT', 'SCHEDULED', 'COMPLETED'].map((status, index) => ({ id: index + 1, code: 'CMP-INFLUENZA-PROTECTION-2026-' + index, name: index === 0 ? 'National Influenza Protection Campaign for Healthcare Personnel 2026' : 'Vaccination Programme ' + (index + 1), status, vaccineTypeCode: 'INFLUENZA-QUAD', targetGroupCode: 'HEALTHCARE-WORKERS', serviceStartDate: '2026-09-01', serviceEndDate: '2026-12-31' }));
  await page.unrouteAll();
  await page.route('**/api/v1/**', async route => {
    const path = new URL(route.request().url()).pathname;
    const data = path.endsWith('/auth/refresh') ? { token: 'browser-preview', user } : path.endsWith('/auth/me') ? user : path.endsWith('/campaigns') ? campaigns : [];
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(data) });
  });
  await page.setViewportSize({ width: 1440, height: 1000 });
  await page.goto('http://localhost:5173/app/campaigns');
  await page.getByRole('heading', { name: 'Vaccination Campaigns', exact: true }).waitFor();
  await page.getByText(campaigns[0].name, { exact: true }).waitFor();
  await page.screenshot({ path: 'output/playwright/campaign-desktop.png', fullPage: true });
  await page.setViewportSize({ width: 390, height: 844 });
  await page.getByRole('button', { name: 'Actions for ' + campaigns[0].name, exact: true }).click();
  await page.screenshot({ path: 'output/playwright/campaign-mobile.png', fullPage: true });
  const overflow = await page.evaluate(() => ({ viewport: innerWidth, document: document.documentElement.scrollWidth }));
  console.log(JSON.stringify(overflow));
}
