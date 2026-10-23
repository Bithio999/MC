#!/usr/bin/env node
/** Export editable projects through real Blockbench + its official GeckoLib plugin.
 * Requires Playwright, Chromium and curl. curl preserves the cloud proxy/CA trust.
 * Third-party editor/plugin code is cached outside the repository, never shipped.
 */
const fs=require('fs'),path=require('path'),os=require('os'),crypto=require('crypto');
const {execFile}=require('child_process');const {promisify}=require('util');const run=promisify(execFile);
const {chromium}=require('playwright');
const root=path.resolve(__dirname,'..'),source=path.join(root,'art/blockbench'),assets=path.join(root,'src/main/resources/assets/wildhunt');
const cache=path.join(os.tmpdir(),'tensura-myths-blockbench-cache');fs.mkdirSync(cache,{recursive:true});
async function download(url){
 const key=path.join(cache,crypto.createHash('sha256').update(url).digest('hex'));if(fs.existsSync(key))return fs.readFileSync(key);
 const {stdout}=await run('curl',['--fail','--silent','--show-error','--location','--max-time','45',url],{encoding:'buffer',maxBuffer:80000000});fs.writeFileSync(key,stdout);return stdout;
}
(async()=>{
 const browser=await chromium.launch({executablePath:process.env.CHROMIUM_PATH||'/usr/bin/chromium',headless:true,args:['--no-sandbox','--use-gl=angle','--use-angle=swiftshader','--enable-unsafe-swiftshader']});
 try{
 const page=await browser.newPage({viewport:{width:1440,height:1000}});const errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.route('**/*',async route=>{
  const u=route.request().url();if(!u.startsWith('http'))return route.continue();
  if(!['web.blockbench.net','cdn.jsdelivr.net','raw.githubusercontent.com','fonts.googleapis.com','fonts.gstatic.com'].includes(new URL(u).hostname))return route.abort();
  try{const body=await download(u),ext=new URL(u).pathname.split('.').pop();await route.fulfill({body,contentType:({js:'application/javascript',css:'text/css',html:'text/html',json:'application/json',png:'image/png',svg:'image/svg+xml',woff2:'font/woff2'})[ext]||'text/html'});}catch(e){await route.abort();}
 });
 await page.goto('https://web.blockbench.net/',{waitUntil:'domcontentloaded',timeout:60000});await page.waitForFunction(()=>window.Formats&&window.Codecs,{timeout:60000});
 await page.addScriptTag({content:(await download('https://raw.githubusercontent.com/JannisX11/blockbench-plugins/master/plugins/geckolib/geckolib.js')).toString()});
 await page.evaluate(async()=>{const p=Plugins.all.find(x=>x.id==='geckolib');if(p&&!p.installed)await p.install();});
 await page.waitForFunction(()=>Formats.geckolib_model,{timeout:30000});
 console.log('Blockbench',await page.evaluate(()=>Blockbench.version),'with GeckoLib plugin');
 const files=fs.readdirSync(source,{recursive:true}).filter(f=>f.endsWith('.bbmodel')&&(!process.env.MODEL||f.startsWith(process.env.MODEL)));
 for(const f of files){
  const model=JSON.parse(fs.readFileSync(path.join(source,f),'utf8'));
  const result=await page.evaluate(async model=>{
   if(window.Project){Project.saved=true;await Project.close();}
   newProject(Formats[model.meta.model_format]);Codecs.project.parse(model);Project.model_identifier=model.model_identifier;
   await new Promise(r=>setTimeout(r,60));Canvas.updateAll();
   if(model.meta.model_format==='java_block'){const item=Codecs.java_block.compile({raw:true});return {item:typeof item==='string'?JSON.parse(item):item};}
   const geo=Codecs.bedrock.compile({raw:true});const animation=Animator.buildFile(null,Animation.all.map(a=>a.name));
   return {geo:typeof geo==='string'?JSON.parse(geo):geo,animation:typeof animation==='string'?JSON.parse(animation):animation,bones:Group.all.length,cubes:Cube.all.length};
  },model);
  if(result.item){fs.writeFileSync(path.join(assets,'models/item',path.basename(f,'.bbmodel')+'.json'),JSON.stringify(result.item,null,2)+'\n');console.log(f,'Java item model');continue;}
  if(!result.geo['minecraft:geometry']?.[0]?.bones?.length||!Object.keys(result.animation.animations||{}).length)throw Error('Empty export: '+f+' '+JSON.stringify(result));
  // Curved-surface bones retain their editor previews in the .bbmodel. Their
  // runtime vertices come from SmoothGeoGeometry, so don't bake invisible cubes.
  for(const bone of result.geo['minecraft:geometry'][0].bones)if(bone.name.startsWith('surface_'))delete bone.cubes;
  const name=f.replace('.bbmodel','');
  fs.writeFileSync(path.join(assets,'geo/myths',name+'.geo.json'),JSON.stringify(result.geo,null,2)+'\n');
  fs.writeFileSync(path.join(assets,'animations/myths',name+'.animation.json'),JSON.stringify(result.animation,null,2)+'\n');
  console.log(name,result.bones+' bones',result.cubes+' cubes');
  if(process.env.CAPTURE&&['ananke','astral_volumes','fate_loom','oil_lash','grand_synthesis_codices'].includes(name)){
   await page.evaluate(()=>{document.querySelector('#dialog_wrapper').style.display='none';Modes.options.edit.select();Preview.selected.camera.position.set(90,70,110);Preview.selected.controls.target.set(0,28,0);Preview.selected.controls.update();Canvas.updateAll();});
   await page.screenshot({path:path.join(process.env.CAPTURE,name+'-blockbench.png')});
  }
 }
 const fatal=errors.filter(s=>!s.includes('ResizeObserver'));if(fatal.length)throw Error('Editor errors: '+fatal.join('\n'));
 console.log('Exported',files.length,'projects through Blockbench.');
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exit(1)});
