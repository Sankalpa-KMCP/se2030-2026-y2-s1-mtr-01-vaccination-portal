import { spawn } from 'child_process';
import fs from 'fs';
import path from 'path';

const chrome = spawn('/Applications/Google Chrome.app/Contents/MacOS/Google Chrome', [
  '--headless=new',
  '--remote-debugging-port=9222',
  '--window-size=1600,1200',
  'http://localhost:5173/'
]);

async function wait(ms) {
  return new Promise(r => setTimeout(r, ms));
}

async function getWsUrl() {
  for (let i = 0; i < 20; i++) {
    try {
      const res = await fetch('http://127.0.0.1:9222/json/list');
      const data = await res.json();
      if (data && data[0] && data[0].webSocketDebuggerUrl) {
        return data[0].webSocketDebuggerUrl;
      }
    } catch (e) {
      await wait(500);
    }
  }
  throw new Error('Timeout waiting for chrome');
}

async function main() {
  try {
    const wsUrl = await getWsUrl();
    const ws = new WebSocket(wsUrl);

    await new Promise((resolve, reject) => {
      ws.onopen = resolve;
      ws.onerror = reject;
    });

    let id = 1;
    const callbacks = new Map();

    ws.onmessage = (event) => {
      const msg = JSON.parse(event.data);
      if (callbacks.has(msg.id)) {
        const cb = callbacks.get(msg.id);
        callbacks.delete(msg.id);
        cb(msg.result);
      }
    };

    function send(method, params = {}) {
      return new Promise((resolve) => {
        const msgId = id++;
        callbacks.set(msgId, resolve);
        ws.send(JSON.stringify({ id: msgId, method, params }));
      });
    }

    const viewports = [1440, 1280, 1024, 768, 390];
    const outDir = '/Users/hirukaliyanage/.gemini/antigravity-ide/brain/be0ace10-af97-4dc1-9d00-7dac84cac56d';

    for (const w of viewports) {
      await send('Emulation.setDeviceMetricsOverride', {
        width: w,
        height: 900,
        deviceScaleFactor: 1,
        mobile: w <= 768
      });
      await wait(600);

      // Check overflow
      const check = await send('Runtime.evaluate', {
        expression: `(() => {
          const docWidth = document.documentElement.clientWidth;
          const scrollWidth = document.documentElement.scrollWidth;
          const overflowing = [];
          document.querySelectorAll('*').forEach(el => {
            const rect = el.getBoundingClientRect();
            if (rect.right > docWidth + 2 || el.scrollWidth > docWidth + 2) {
              overflowing.push({
                tag: el.tagName,
                className: String(el.className).slice(0, 40),
                rectRight: Math.round(rect.right),
                scrollWidth: el.scrollWidth
              });
            }
          });
          return { width: docWidth, scrollWidth, overflowCount: overflowing.length, overflowing: overflowing.slice(0, 5) };
        })()`,
        returnByValue: true
      });

      console.log(`Viewport ${w}px (Normal Text):`, JSON.stringify(check.result.value));

      // Capture screenshot
      const shot = await send('Page.captureScreenshot', { format: 'png' });
      fs.writeFileSync(path.join(outDir, `verified_${w}px.png`), Buffer.from(shot.data, 'base64'));

      // Check with A++ enlarged text
      await send('Runtime.evaluate', {
        expression: `document.documentElement.className = 'vax-text-xl';`
      });
      await wait(400);

      const checkXl = await send('Runtime.evaluate', {
        expression: `(() => {
          const docWidth = document.documentElement.clientWidth;
          const scrollWidth = document.documentElement.scrollWidth;
          const overflowing = [];
          document.querySelectorAll('*').forEach(el => {
            const rect = el.getBoundingClientRect();
            if (rect.right > docWidth + 2 || el.scrollWidth > docWidth + 2) {
              overflowing.push({
                tag: el.tagName,
                className: String(el.className).slice(0, 40),
                rectRight: Math.round(rect.right),
                scrollWidth: el.scrollWidth
              });
            }
          });
          return { width: docWidth, scrollWidth, overflowCount: overflowing.length, overflowing: overflowing.slice(0, 5) };
        })()`,
        returnByValue: true
      });

      console.log(`Viewport ${w}px (A++ Enlarged Text):`, JSON.stringify(checkXl.result.value));

      const shotXl = await send('Page.captureScreenshot', { format: 'png' });
      fs.writeFileSync(path.join(outDir, `verified_${w}px_text_xl.png`), Buffer.from(shotXl.data, 'base64'));

      // Reset text size
      await send('Runtime.evaluate', {
        expression: `document.documentElement.className = '';`
      });
    }

    ws.close();
  } catch (err) {
    console.error(err);
  } finally {
    chrome.kill();
  }
}

main();
