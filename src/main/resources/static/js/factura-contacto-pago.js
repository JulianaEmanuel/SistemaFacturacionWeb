(() => {
    const cliente=document.getElementById('idCliente');
    if(!cliente)return;
    const pintar=()=>{
        const option=cliente.selectedOptions[0];
        for(const [campo,titulo] of [['direccion','Dirección'],['correo','Correo'],['telefono','Teléfono']]) {
            const destino=document.getElementById(`cliente-contacto-${campo}`);
            if(destino) destino.textContent=`${titulo}: ${option?.dataset[campo] || 'No registrado'}`;
        }
    };
    cliente.addEventListener('change',pintar);pintar();
    const total=document.getElementById('factura-total'),pago=document.getElementById('factura-pago-importe');
    if(total&&pago){const actualizar=()=>{pago.textContent=`$ ${total.textContent}`;};new MutationObserver(actualizar).observe(total,{childList:true,characterData:true,subtree:true});actualizar();}
})();
