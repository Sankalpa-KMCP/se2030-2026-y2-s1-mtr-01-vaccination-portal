async (page) => {
  const user = { id: 1, fullName: 'Preview Administrator', roles: ['ROLE_ADMIN'] };
  const statuses = ['ACTIVE', 'DRAFT', 'SCHEDULED', 'PUBLISHED', 'SUSPENDED', 'COMPLETED', 'CANCELLED', 'ARCHIVED'];
  const campaigns = statuses.map((status, index) => ({ id: index + 1, code: 'CMP-INFLUENZA-PROTECTION-2026-' + index, name: index === 0 ? 'National Influenza Protection Campaign for Healthcare Personnel 2026' : 'Vaccination Programme ' + (index + 1), status, vaccineTypeCode: 'INFLUENZA-QUAD', targetGroupCode: 'HEALTHCARE-WORKERS', serviceStartDate: '2026-09-01', serviceEndDate: '2026-12-31' }));
  let mode = 'normal';
  await page.unrouteAll();
  await page.route('**/api/v1/**', async route => {
    const url = new URL(route.request().url());
    const path = url.pathname;
    if (path.endsWith('/campaigns') && mode === 'error') return route.fulfill({status: 503, contentType: 'application/json', body: JSON.stringify({message:'Preview service unavailable'})});
    const data = path.endsWith('/auth/refresh') ? { token: 'browser-preview', user } : path.endsWith('/auth/me') ? user : path.endsWith('/campaigns') ? mode === 'empty' ? [] : campaigns.filter(c => !url.searchParams.get('status') || c.status === url.searchParams.get('status')) : [];
    await route.fulfill({status:200, contentType:'application/json', body:JSON.stringify(data)});
  });
  const report = [];
  for (const width of [1440, 1024, 768, 390, 320]) {
    await page.setViewportSize({width, height:1000});
    await page.goto('http://localhost:5173/app/campaigns');
    await page.getByText(campaigns[0].name, {exact:true}).waitFor();
    await page.getByRole('button', {name:'Actions for ' + campaigns[0].name, exact:true}).click();
    await page.getByRole('button', {name:'Suspend', exact:true}).waitFor();
    const bounds = await page.evaluate(() => {
      const rect = document.querySelector('.campaign-action-menu').getBoundingClientRect();
      return {width:innerWidth, document:document.documentElement.scrollWidth, menuLeft:rect.left, menuRight:rect.right};
    });
    if (bounds.document > width || bounds.menuLeft < 0 || bounds.menuRight > width) throw new Error(JSON.stringify(bounds));
    report.push(bounds);
    await page.getByRole('button', {name:'Suspend', exact:true}).focus();
    await page.keyboard.press('Escape');
    if (!(await page.getByRole('button', {name:'Actions for ' + campaigns[0].name, exact:true}).evaluate(el => el === document.activeElement))) throw new Error('Escape focus failed');
    if (width === 1440 || width === 390) await page.screenshot({path:'output/playwright/premium-campaign-' + width + '.png', fullPage:true, animations:'disabled'});
  }
  await page.setViewportSize({width:720,height:500});
  await page.screenshot({path:'output/playwright/premium-campaign-zoom-reflow.png', fullPage:true, animations:'disabled'});
  user.roles = ['ROLE_MANAGEMENT_USER'];
  await page.reload(); await page.getByText(campaigns[0].name, {exact:true}).waitFor();
  if (await page.getByRole('button', {name:/Actions for/}).count()) throw new Error('Read-only actions exposed');
  mode = 'empty'; await page.reload(); await page.getByText('No campaigns yet', {exact:true}).waitFor();
  mode = 'error'; await page.reload(); await page.getByText('Results unavailable', {exact:true}).waitFor();
  return report;
}

