async (page) => {
  await page.unrouteAll();
  const user = {id:1, fullName:'Preview Administrator', roles:['ROLE_ADMIN']};
  let loadingRoute = false;
  let releaseLoad;
  const pendingLoad = new Promise(resolve => {releaseLoad = resolve;});
  await page.route('**/api/v1/**', async route => {
    const path = new URL(route.request().url()).pathname;
    if (path.endsWith('/campaigns') && loadingRoute) await pendingLoad;
    const data = path.endsWith('/auth/refresh') ? {token:'preview',user} : path.endsWith('/auth/me') ? user : path.endsWith('/campaigns') ? [{id:1,name:'Preview campaign',code:'CMP-001',status:'ACTIVE'}] : [];
    await route.fulfill({status:200,contentType:'application/json',body:JSON.stringify(data)});
  });
  const session = await page.context().newCDPSession(page);
  await session.send('Emulation.setDeviceMetricsOverride', {width:720,height:500,deviceScaleFactor:2,mobile:false,screenWidth:1440,screenHeight:1000});
  await page.goto('http://localhost:5173/app/campaigns');
  await page.getByText('Preview campaign',{exact:true}).waitFor();
  const zoom = await page.evaluate(() => ({width:innerWidth,document:document.documentElement.scrollWidth,dpr:devicePixelRatio}));
  if (zoom.document > zoom.width) throw new Error('Zoom overflow');
  await page.screenshot({path:'output/playwright/premium-campaign-200-percent.png',fullPage:true,animations:'disabled'});
  await page.emulateMedia({reducedMotion:'reduce'});
  const motion = await page.locator('.campaign-banner').evaluate(el => getComputedStyle(el).animationName);
  if(motion !== 'none') throw new Error('Reduced motion ignored');
  await page.getByLabel('Search campaigns').fill('does-not-exist');
  await page.getByText('No matching campaigns',{exact:true}).waitFor();
  await page.getByRole('button',{name:'Clear filters'}).first().click();
  loadingRoute=true;
  await page.getByLabel('Status').selectOption('ACTIVE');
  await page.getByText('Loading campaigns…',{exact:true}).waitFor();
  await page.screenshot({path:'output/playwright/premium-campaign-loading.png',fullPage:true,animations:'disabled'});
  releaseLoad();
  await page.getByText('Preview campaign',{exact:true}).waitFor();
  const contrast = await page.evaluate(() => {
    const luminance = (c) => { const rgb=c.match(/\d+/g).slice(0,3).map(Number).map(v=>{v/=255;return v<=.04045?v/12.92:((v+.055)/1.055)**2.4;});return rgb[0]*.2126+rgb[1]*.7152+rgb[2]*.0722; };
    const selectors=['.campaign-banner h1','.campaign-banner-copy > p','.campaign-header-help','.campaign-name','.campaign-code','.campaign-table th'];
    return selectors.map(selector=>{const el=document.querySelector(selector);const fg=getComputedStyle(el).color;const bg=selector.startsWith('.campaign-banner')||selector==='.campaign-header-help'?'rgb(10,107,89)':selector==='.campaign-table th'?'rgb(245,249,246)':'rgb(255,255,255)';const a=luminance(fg), b=luminance(bg);return {selector,ratio:(Math.max(a,b)+.05)/(Math.min(a,b)+.05)};});
  });
  if(contrast.some(c=>c.ratio<4.5)) throw new Error(JSON.stringify(contrast));
  await session.send('Emulation.clearDeviceMetricsOverride');
  return {zoom, motion, contrast};
}
