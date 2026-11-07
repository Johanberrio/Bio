'use strict';
const form = document.getElementById('deletion');
const statusText = document.getElementById('status');
const button = document.getElementById('submit');
form.addEventListener('submit', async event => {
  event.preventDefault();
  if (!form.reportValidity()) return;
  button.disabled = true;
  statusText.textContent = 'Verificando la cuenta…';
  let idToken;
  try {
    const configResponse = await fetch('./account-config', {cache:'no-store'});
    if (!configResponse.ok) throw new Error('No se pudo contactar con Bio. Inténtalo más tarde.');
    const config = await configResponse.json();
    const authResponse = await fetch('https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key='+encodeURIComponent(config.apiKey), {
      method:'POST', headers:{'Content-Type':'application/json'},
      body:JSON.stringify({email:document.getElementById('email').value.trim(),password:document.getElementById('password').value,returnSecureToken:true})
    });
    document.getElementById('password').value='';
    if (!authResponse.ok) throw new Error('No se pudo verificar la cuenta. Revisa tus datos o contacta con soporte si ya solicitaste la eliminación.');
    const session = await authResponse.json();
    idToken = session.idToken;
    statusText.textContent='Registrando tu solicitud…';
    const response=await fetch('./account/deletion-request',{
      method:'POST',headers:{'Content-Type':'application/json','Authorization':'Bearer '+idToken},body:JSON.stringify({confirm:true})
    });
    const result=await response.json();
    if(!response.ok||result.requested!==true)throw new Error(result.error||'No se pudo registrar la solicitud. Inténtalo de nuevo.');
    form.hidden=true;
    statusText.textContent=result.message;
  } catch(error) {
    statusText.textContent=error instanceof TypeError?'No se pudo conectar. Comprueba tu conexión e inténtalo de nuevo.':error.message;
    button.disabled=false;
  } finally {
    idToken=undefined;
    document.getElementById('password').value='';
  }
});
