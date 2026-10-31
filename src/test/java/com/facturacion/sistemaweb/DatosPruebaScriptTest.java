package com.facturacion.sistemaweb;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import com.facturacion.sistemaweb.model.Producto;
import com.facturacion.sistemaweb.service.FacturaCalculoService;

/** Lee el archivo como texto; nunca abre una conexion ni ejecuta SQL. */
class DatosPruebaScriptTest {
    @Test void veinteDetallesDelArchivoCoincidenConElCalculadorReal() throws Exception {
        String sql=Files.readString(Path.of("DATOS_PRUEBA.sql"),StandardCharsets.UTF_8);
        String productos=sql.substring(sql.indexOf("INSERT @Productos VALUES"),sql.indexOf("DECLARE @Fechas"));
        var patronProducto=Pattern.compile("\\((\\d+),'([^']+)',(\\d+\\.\\d{4}),(\\d+\\.\\d{2})\\)").matcher(productos);
        var mapa=new HashMap<Integer,Producto>();
        while(patronProducto.find()) {
            var p=new Producto();
            assertTrue(patronProducto.group(2).length()<=30);
            p.setPrecioUnitario(new BigDecimal(patronProducto.group(3)));
            p.setIvaProducto(new BigDecimal(patronProducto.group(4)));
            assertNull(mapa.put(Integer.valueOf(patronProducto.group(1)),p));
        }
        assertEquals(10,mapa.size());
        String cantidades=sql.substring(sql.indexOf("INSERT @Cantidades VALUES"),sql.indexOf("DECLARE @Lineas"));
        var lineas=Pattern.compile("\\((\\d+),(\\d+),(\\d+),(\\d+\\.\\d{2})\\)").matcher(cantidades);
        var conteos=new HashMap<Integer,Integer>();
        var unicos=new HashSet<String>();
        var calculo=new FacturaCalculoService();
        BigDecimal subtotal=new BigDecimal("0.0000"),iva=subtotal,total=subtotal;
        int detalles=0;
        while(lineas.find()) {
            int factura=Integer.parseInt(lineas.group(1)),producto=Integer.parseInt(lineas.group(3));
            assertTrue(unicos.add(factura+":"+producto));
            var resultado=calculo.calcular(mapa.get(producto),new BigDecimal(lineas.group(4)));
            subtotal=subtotal.add(resultado.subtotal());iva=iva.add(resultado.iva());total=total.add(resultado.total());
            conteos.merge(factura,1,Integer::sum);detalles++;
        }
        assertEquals(20,detalles);assertEquals(10,conteos.size());
        conteos.values().forEach(n->assertEquals(2,n.intValue()));
        assertEquals(new BigDecimal("106.3980"),subtotal);
        assertEquals(new BigDecimal("12.6413"),iva);
        assertEquals(new BigDecimal("119.0393"),total);
    }
}
