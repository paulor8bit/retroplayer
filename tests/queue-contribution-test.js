const fs=require('fs'),vm=require('vm'),assert=require('assert');
const nodes=new Map(),requests=[],uploads=[];
const el=()=>({value:'',textContent:'',children:[],files:[],disabled:false,hidden:false,readyState:1,currentTime:0,duration:100,paused:true,append(...items){this.children.push(...items)},replaceChildren(){this.children=[]},load(){},removeAttribute(){},pause(){this.paused=true},async play(){this.paused=false}});
const document={getElementById(id){if(!nodes.has(id))nodes.set(id,el());return nodes.get(id)},createElement:el};
class XHR{
 constructor(){this.upload={};this.headers={};uploads.push(this)}
 open(method,path){this.method=method;this.path=path}
 setRequestHeader(key,value){this.headers[key]=value}
 send(file){this.file=file}
 abort(){this.aborted=true;if(this.onabort)this.onabort()}
}
let closed=false,queue=[{index:0,current:true,name:'<script>alert(1)</script>',author:'Amigo',source:'Arquivo'}];
const context=vm.createContext({document,XMLHttpRequest:XHR,localStorage:{getItem(){return ''},setItem(){}},performance:{now:()=>0},setTimeout(){},Date,Number,Math,String,Infinity,fetch:async(path,options)=>{
 if(options&&options.method==='POST'){requests.push({path,options});return{ok:true,text:async()=> 'Música enviada para a fila.'}}
 return{status:closed?404:200,ok:!closed,json:async()=>path==='queue'?queue:path==='chat'?[]:{id:1,hasMedia:false,title:'Aguardando',position:0,duration:0,playing:false}};
}});
const html=fs.readFileSync('app/src/main/assets/guest.html','utf8');for(const match of html.matchAll(/id="([^"]+)"/g))document.getElementById(match[1]);
vm.runInContext(html.split('<script>')[1].split('</script>')[0],context);
(async()=>{await new Promise(r=>setImmediate(r));const row=nodes.get('sharedQueue').children[0];assert.equal(row.children[1].children[0].textContent,'<script>alert(1)</script>','queue names render as text');nodes.get('name').value='Convidado';nodes.get('queueUrl').value='https://music.youtube.com/watch?v=dQw4w9WgXcQ';nodes.get('queueTitle').value='Minha escolha';await nodes.get('addQueue').onsubmit({preventDefault(){}});assert.equal(requests.length,1);assert.equal(requests[0].path,'queue');assert.equal(JSON.parse(requests[0].options.body).name,'Convidado');assert.equal(nodes.get('queueUrl').value,'');assert.equal(nodes.get('media').paused,true,'contribution does not start or interrupt playback');
 const file={name:'Música local.mp3',size:32000};nodes.get('audioFile').files=[file];nodes.get('audioFile').onchange();const upload=uploads[0];assert.equal(upload.file,file);assert.equal(upload.headers['X-File-Name'],encodeURIComponent(file.name));assert.equal(upload.headers['X-Guest-Name'],'Convidado');assert.equal(nodes.get('audioFile').disabled,true);upload.upload.onprogress({lengthComputable:true,loaded:16000,total:32000});assert.equal(nodes.get('uploadProgress').value,50);upload.status=202;upload.responseText='Arquivo recebido';upload.onload();assert.equal(nodes.get('audioFile').disabled,false);assert.equal(nodes.get('queueStatus').textContent,'Arquivo recebido');nodes.get('audioFile').files=[{name:'big.mp3',size:101*1024*1024}];nodes.get('audioFile').onchange();assert.equal(uploads.length,1,'oversize file rejected before upload');nodes.get('audioFile').files=[file];nodes.get('audioFile').onchange();closed=true;await vm.runInContext('poll()',context);assert.equal(uploads[1].aborted,true);assert.equal(nodes.get('audioFile').disabled,true);console.log('PASS: safe queue rendering, guest link submission, upload headers/progress/limits and closure cancellation')})().catch(e=>{console.error(e);process.exitCode=1});
