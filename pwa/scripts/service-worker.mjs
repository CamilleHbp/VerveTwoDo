import { readdir, readFile, writeFile } from "node:fs/promises";
import { createHash } from "node:crypto";
const files = await readdir("dist", { recursive: true, withFileTypes: true });
const assets = files
  .filter((f) => f.isFile() && f.name !== "sw.js")
  .map((f) => `${f.parentPath}/${f.name}`.replace(/^dist/, ""))
  .sort();
const hash = createHash("sha256");
for (const path of assets) hash.update(await readFile(`dist${path}`));
const version = hash.digest("hex").slice(0, 16);
await writeFile(
  "dist/sw.js",
  `const CACHE='vervetwodo-${version}';const ASSETS=${JSON.stringify(assets)};
self.addEventListener('install',event=>event.waitUntil(caches.open(CACHE).then(cache=>cache.addAll(ASSETS))));
self.addEventListener('activate',event=>event.waitUntil(Promise.all([caches.keys().then(keys=>Promise.all(keys.filter(key=>key.startsWith('vervetwodo-')&&key!==CACHE).map(key=>caches.delete(key)))),self.clients.claim()])));
self.addEventListener('message',event=>{if(event.data==='activate')self.skipWaiting();});
self.addEventListener('push',event=>{let message;try{message=event.data.json();}catch{return;}event.waitUntil(self.registration.showNotification(message.title,{body:message.body,tag:message.tag,icon:'/icons/app-192.png',data:{taskId:message.taskId}}));});
self.addEventListener('fetch',event=>{const url=new URL(event.request.url);if(event.request.method!=='GET'||url.origin!==self.location.origin||url.pathname.startsWith('/api/'))return;if(event.request.mode==='navigate'){event.respondWith(caches.open(CACHE).then(cache=>cache.match('/index.html')).then(cached=>cached||fetch(event.request)));return;}if(ASSETS.includes(url.pathname))event.respondWith(caches.open(CACHE).then(cache=>cache.match(url.pathname)).then(cached=>cached||fetch(event.request)));});
self.addEventListener('notificationclick',event=>{event.notification.close();const taskId=event.notification.data?.taskId;event.waitUntil(self.clients.matchAll({type:'window',includeUncontrolled:true}).then(async clients=>{if(clients.length){await clients[0].focus();clients[0].postMessage({taskId});}else await self.clients.openWindow('/'+(taskId?'?task='+encodeURIComponent(taskId):''));}));});
`,
);
console.log(`Offline shell: ${assets.length} assets, ${version}`);
