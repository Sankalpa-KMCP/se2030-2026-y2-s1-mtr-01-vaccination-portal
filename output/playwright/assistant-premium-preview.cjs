async (page) => {
  await page.unrouteAll();
  await page.route('**/api/v1/**', async route => {
    const path = new URL(route.request().url()).pathname;
    if (path.endsWith('/assistant/status')) return route.fulfill({json:{available:true,personalizedFeaturesAvailable:false,disclaimer:'This assistant explains portal services. It does not provide medical advice or emergency care.'}});
    if (path.endsWith('/assistant/messages')) return route.fulfill({json:{answer:'You can book your vaccination in a few simple steps.\n\n1. Complete screening for your chosen campaign.\n2. Choose an available session at a vaccination center.\n3. Confirm your appointment.\n\nI can take you to the booking page when you are ready.',suggestedActions:[{label:'Explore appointments',path:'/app/appointments/book'}]}});
    return route.fulfill({status:401,json:{message:'Not signed in'}});
  });
  await page.setViewportSize({width:1440,height:960});
  await page.goto('http://127.0.0.1:5175/');
  await page.getByRole('button',{name:'Open portal assistant',exact:true}).click();
  await page.getByText('Ready to help',{exact:true}).waitFor();
  await page.getByRole('heading',{name:'How can I help?',exact:true}).waitFor();
  await page.screenshot({path:'output/playwright/assistant-premium-desktop.png'});
  await page.getByRole('dialog',{name:'Portal Assistant',exact:true}).screenshot({path:'output/playwright/assistant-premium-panel.png'});
  await page.setViewportSize({width:768,height:1024});
  await page.screenshot({path:'output/playwright/assistant-premium-tablet.png'});
  await page.setViewportSize({width:390,height:844});
  await page.screenshot({path:'output/playwright/assistant-premium-mobile.png'});
  console.log(await page.getByRole('dialog').evaluate(el=>({panel:el.getBoundingClientRect().toJSON(),overflow:el.scrollWidth>el.clientWidth})));
  await page.getByRole('button',{name:'How do I book an appointment?',exact:true}).click();
  await page.getByRole('button',{name:'Explore appointments',exact:true}).waitFor();
  await page.screenshot({path:'output/playwright/assistant-premium-mobile-conversation.png'});
}


