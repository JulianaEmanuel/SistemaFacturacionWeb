const test=require('node:test'),assert=require('node:assert/strict'),vm=require('node:vm'),fs=require('node:fs'),path=require('node:path');
const source=fs.readFileSync(path.resolve(__dirname,'../../main/resources/static/js/factura-contacto-pago.js'),'utf8');
test('contactos se presentan como texto e importe sigue al total sin enviar cantidades',()=>{
 let change,refresh;
 const cliente={selectedOptions:[{dataset:{direccion:'<img src=x>',correo:'cliente@example.invalid'}}],addEventListener(_,f){change=f;}};
 const elements={idCliente:cliente,'cliente-contacto-direccion':{},'cliente-contacto-correo':{},'cliente-contacto-telefono':{},'factura-total':{textContent:'4.60'},'factura-pago-importe':{}};
 class Observer{constructor(f){refresh=f;}observe(){}}
 vm.runInNewContext(source,{document:{getElementById:id=>elements[id]},MutationObserver:Observer});
 assert.equal(elements['cliente-contacto-direccion'].textContent,'Dirección: <img src=x>');assert.equal(elements['cliente-contacto-telefono'].textContent,'Teléfono: No registrado');assert.equal(elements['factura-pago-importe'].textContent,'$ 4.60');
 elements['factura-total'].textContent='12.25';refresh();assert.equal(elements['factura-pago-importe'].textContent,'$ 12.25');cliente.selectedOptions=[];change();assert.equal(elements['cliente-contacto-correo'].textContent,'Correo: No registrado');
});
test('puede cargarse sin formulario',()=>{assert.doesNotThrow(()=>vm.runInNewContext(source,{document:{getElementById(){return null;}}}));});
