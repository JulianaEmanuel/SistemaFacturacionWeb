package com.facturacion.sistemaweb.service;

import java.io.*;
import java.util.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import com.facturacion.sistemaweb.model.*;

@Service @Profile("sqlserver") @PreAuthorize("isAuthenticated()")
public class ReportePdfService {
    private final ReporteService reportes;
    public ReportePdfService(ReporteService reportes) { this.reportes=reportes; }
    public byte[] generar(ReporteTipo tipo,ReporteFiltro filtro) {
        var data=reportes.consultar(tipo,filtro);ReporteService.exigirLimite(data.filas().size());
        try(var doc=new PDDocument();var bytes=new ByteArrayOutputStream()) {
            PDFont font;
            try(InputStream fuente=ReportePdfService.class.getResourceAsStream("/org/apache/pdfbox/resources/ttf/LiberationSans-Regular.ttf")) {
                font=fuente==null?new PDType1Font(Standard14Fonts.FontName.HELVETICA):PDType0Font.load(doc,fuente,true);
            }
            try(var writer=new Tabla(doc,font,data)) {
                writer.nuevaPagina();
                for(var f:data.filas()) writer.fila(new String[]{f.id(),f.nombre(),f.referencia(),Long.toString(f.facturas()),f.cantidadVisible(),f.subtotalVisible(),f.ivaVisible(),f.totalVisible()});
                if(data.sinVentas()) writer.nota("Sin movimientos en el período y alcance consultados.");
            }
            for(int i=0;i<doc.getNumberOfPages();i++) try(var stream=new PDPageContentStream(doc,doc.getPage(i),PDPageContentStream.AppendMode.APPEND,true,true)) {
                texto(stream,font,8,35,20,"Página "+(i+1)+" de "+doc.getNumberOfPages());
            }
            doc.getDocumentInformation().setTitle(tipo.getTitulo());doc.getDocumentInformation().setCreator("Sistema de Facturación Web");
            doc.save(bytes);return bytes.toByteArray();
        } catch(IOException ex) { throw new ReporteException("No se pudo generar el PDF. Inténtalo nuevamente."); }
    }
    private static String seguro(PDFont font,String raw) throws IOException {
        StringBuilder out=new StringBuilder();
        for(int cp:raw.codePoints().toArray()) {
            if(Character.isISOControl(cp)) { out.append(' ');continue; }
            String text=new String(Character.toChars(cp));
            try { font.encode(text);out.append(text); } catch(IllegalArgumentException ex) { out.append('?'); }
        }
        return out.toString();
    }
    private static void texto(PDPageContentStream stream,PDFont font,float size,float x,float y,String text) throws IOException {
        stream.beginText();stream.setFont(font,size);stream.newLineAtOffset(x,y);stream.showText(seguro(font,text));stream.endText();
    }
    private static List<String> partir(PDFont font,String raw,float ancho,float size) throws IOException {
        String text=seguro(font,raw);List<String> lines=new ArrayList<>();StringBuilder actual=new StringBuilder();
        for(int cp:text.codePoints().toArray()) {
            String next=new String(Character.toChars(cp));
            if(actual.length()>0 && font.getStringWidth(actual+next)*size/1000>ancho) { lines.add(actual.toString());actual.setLength(0); }
            actual.append(next);
        }
        if(actual.length()>0) lines.add(actual.toString());if(lines.isEmpty()) lines.add("");return lines;
    }
    private static class Tabla implements AutoCloseable {
        final PDDocument doc;final PDFont font;final ReporteResultado data;
        final float[] widths={65,180,160,52,65,80,80,80};
        PDPageContentStream stream;float y;
        Tabla(PDDocument doc,PDFont font,ReporteResultado data) { this.doc=doc;this.font=font;this.data=data; }
        void nuevaPagina() throws IOException {
            if(stream!=null) stream.close();
            PDPage page=new PDPage(new PDRectangle(PDRectangle.A4.getHeight(),PDRectangle.A4.getWidth()));doc.addPage(page);stream=new PDPageContentStream(doc,page);y=page.getMediaBox().getHeight()-35;
            stream.setNonStrokingColor(21/255f,133/255f,106/255f);texto(stream,font,16,35,y,"Sistema de Facturación Web");stream.setNonStrokingColor(0f);y-=22;
            texto(stream,font,12,35,y,data.tipo().getTitulo());y-=17;
            linea("Período: "+data.filtro().desde()+" a "+data.filtro().hasta()+" | "+data.alcance(),9);
            linea(data.filtro().descripcion(),8);
            var r=data.resumen();
            linea("Resumen del alcance filtrado: "+r.facturas()+" facturas | Subtotal "+r.subtotalVisible()+" | IVA "+r.ivaVisible()+" | Total "+r.totalVisible()+" | Ticket promedio "+r.ticketVisible(),9);
            if(data.tipo()==ReporteTipo.PRODUCTOS) linea("El resumen abarca todos los productos filtrados; la tabla muestra el ranking seleccionado.",8);
            y-=8;float x=35;
            String[] titulos={"ID / período","Nombre / descripción","Referencia / cédula","Facturas","Cantidad","Subtotal","IVA","Total"};
            stream.setNonStrokingColor(.88f,.95f,.91f);stream.addRect(35,y-24,762,28);stream.fill();stream.setNonStrokingColor(0f);
            for(int i=0;i<titulos.length;i++) { texto(stream,font,8,x+3,y-9,titulos[i]);x+=widths[i]; }
            y-=32;
        }
        void linea(String line,float size) throws IOException {
            for(String part:partir(font,line,762,size)) { texto(stream,font,size,35,y,part);y-=size+4; }
        }
        void fila(String[] cells) throws IOException {
            List<List<String>> lines=new ArrayList<>();int max=1;
            for(int i=0;i<cells.length;i++) { var partes=partir(font,cells[i],widths[i]-7,8);lines.add(partes);max=Math.max(max,partes.size()); }
            float altura=max*11+10;
            if(y-altura<40) nuevaPagina();
            float x=35;
            for(int i=0;i<cells.length;i++) {
                float lineY=y;
                for(String line:lines.get(i)) { texto(stream,font,8,x+3,lineY,line);lineY-=11; }
                x+=widths[i];
            }
            stream.setStrokingColor(.8f);stream.moveTo(35,y-altura+6);stream.lineTo(797,y-altura+6);stream.stroke();y-=altura;
        }
        void nota(String text) throws IOException { if(y<65) nuevaPagina();linea(text,9); }
        @Override public void close() throws IOException { if(stream!=null) stream.close(); }
    }
}
