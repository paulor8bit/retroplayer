const fs=require('fs'),vm=require('vm'),assert=require('assert');
const nodes=new Map(),buffers=[],contexts=[];
const el=()=>({value:'1',disabled:false,hidden:false,readyState:1,currentTime:0,duration:100,paused:true,playbackRate:1,append(){},replaceChildren(){},load(){},removeAttribute(){delete this.src},pause(){this.paused=true},async play(){this.paused=false}});
const document={getElementById(id){if(!nodes.has(id))nodes.set(id,el());return nodes.get(id)},createElement:el,head:{append(){}}};
class AudioContext{
 constructor(){this.state='running';this.currentTime=0;this.closed=false;contexts.push(this)}
 createGain(){return{gain:{value:1},connect(){}}}
 async resume(){}
 async close(){this.closed=true}
 createBuffer(channels,frames,rate){const data=Array.from({length:channels},()=>new Float32Array(frames));const b={duration:frames/rate,getChannelData(i){return data[i]}};buffers.push(b);return b}
 createBufferSource(){return{connect(){},start(){},stop(){}}}
}
let state={live:true,id:'live:one',hasMedia:true,playing:true,title:'Som do dispositivo',sampleRate:44100,channels:2,position:0,duration:0,captureStatus:'Transmitindo'},closed=false,liveCalls=0;
const context=vm.createContext({window:{AudioContext},location:{origin:'https://room.test',href:'https://room.test/token/',pathname:'/token/',hash:'#app'},document,AbortController,localStorage:{getItem(){return ''}},performance:{now:()=>0},setTimeout(){},Date,Number,Math,String,Infinity,fetch:async(path,options)=>{
 if(path==='live'){liveCalls++;let n=0;return{ok:true,body:{getReader(){return{async read(){if(n++===0)return{done:false,value:new Uint8Array([0,64,0])};if(n===2)return{done:false,value:new Uint8Array([192,0,32,0,224])};return new Promise(()=>{})},releaseLock(){}}}}}}
 return{status:closed?404:200,ok:!closed,json:async()=>path==='chat'||path==='queue'?[]:state};
}});
vm.runInContext(fs.readFileSync('app/src/main/assets/guest.html','utf8').split('<script>')[1].split('</script>')[0],context);
async function flush(){await new Promise(r=>setImmediate(r));}
(async()=>{await flush();assert.equal(liveCalls,0,'no live playback before gesture');await nodes.get('join').onclick();await flush();assert.equal(liveCalls,1);assert.equal(buffers.length,1,'split frame reassembled');assert.equal(buffers[0].getChannelData(0)[0],.5);assert.equal(buffers[0].getChannelData(1)[0],-.5);assert.equal(buffers[0].getChannelData(0)[1],.25);assert.equal(buffers[0].getChannelData(1)[1],-.25);assert.equal(nodes.get('media').src,undefined,'live audio does not request native media');nodes.get('volume').value='.3';nodes.get('volume').oninput({target:{value:'.3'}});assert.equal(vm.runInContext('liveAudio.gain.gain.value',context),.3);state={...state,id:'native:2',live:false,position:2,duration:100};await vm.runInContext('poll()',context);assert.equal(contexts[0].closed,true,'source switch stops capture');state={...state,id:'live:two',live:true};await vm.runInContext('poll()',context);await flush();assert.equal(liveCalls,2,'capture can restart');closed=true;await vm.runInContext('poll()',context);assert.equal(contexts[1].closed,true,'room closure stops audio');assert.equal(nodes.get('addLink').disabled,true);assert.equal(nodes.get('audioFile').disabled,true);console.log('PASS: live gesture, PCM stereo/frame decoding, volume, source switches and revocation')})().catch(e=>{console.error(e);process.exitCode=1});
