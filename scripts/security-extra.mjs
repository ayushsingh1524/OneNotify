import assert from 'node:assert/strict';import {randomUUID} from 'node:crypto';
const base=process.env.API_URL||'http://localhost:8080/api/v1';const mail=process.env.MAILPIT_URL||'http://localhost:8025';
const email=`security-${randomUUID()}@example.test`;const password='Security-test-2026!';
async function req(path,{body,headers={},method='POST',status=200}={}){const r=await fetch(base+path,{method,headers:{'Content-Type':'application/json',...headers},body:body?JSON.stringify(body):undefined});assert.equal(r.status,status,`${method} ${path}`);return r;}
let r=await req('/auth/register',{body:{email,password,name:'Security test'}});const session=await r.json();const cookie=r.headers.get('set-cookie').split(';')[0];
await req('/auth/refresh',{headers:{Cookie:cookie,'Sec-Fetch-Site':'cross-site'},status:403});
let rotated=await req('/auth/refresh',{headers:{Cookie:cookie}});const rotatedSession=await rotated.json();const nextCookie=rotated.headers.get('set-cookie').split(';')[0];await req('/auth/refresh',{headers:{Cookie:cookie},status:401});
await req('/cases',{method:'GET',headers:{Authorization:`Bearer ${session.accessToken}`},status:401});
await req('/cases',{method:'GET',headers:{Authorization:`Bearer ${rotatedSession.accessToken}`}});
await req('/auth/forgot-password',{body:{email}});let inbox=await (await fetch(`${mail}/api/v1/messages`)).json();const message=inbox.messages.find(m=>m.To.some(t=>t.Address===email));assert(message,'Reset email captured in local Mailpit');const content=await (await fetch(`${mail}/api/v1/message/${message.ID}`)).json();const token=content.Text.match(/token=([A-Za-z0-9_-]+)/)[1];
await req('/auth/reset-password',{body:{token,password:'Changed-security-2026!'}});await req('/auth/reset-password',{body:{token,password:'Changed-security-2026!'},status:400});await req('/auth/login',{body:{email,password},status:401});const fresh=await req('/auth/login',{body:{email,password:'Changed-security-2026!'}});const freshSession=await fresh.json();const freshCookie=fresh.headers.get('set-cookie').split(';')[0];await req('/auth/refresh',{headers:{Cookie:nextCookie},status:401});
await req('/admin/metrics',{method:'GET',headers:{Authorization:`Bearer ${freshSession.accessToken}`},status:403});await req('/auth/register',{body:{email:`null-${randomUUID()}@example.test`,name:'Missing password'},status:400});
console.log('PASS: refresh rotation/replay, cross-site rejection, real local reset email, reset single-use, old password rejection, refresh revocation, admin restriction, null-password validation.');

await req('/cases',{method:'GET',headers:{Authorization:`Bearer ${rotatedSession.accessToken}`},status:401});
await req('/auth/logout',{headers:{Cookie:freshCookie}});
await req('/cases',{method:'GET',headers:{Authorization:`Bearer ${freshSession.accessToken}`},status:401});
console.log('PASS: access tokens rejected immediately after refresh rotation, password reset and logout.');
