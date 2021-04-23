package ru.fenix2k;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;

import java.io.FileOutputStream;
import java.io.IOException;

public class PdfPageNumeration {
    public static final String FONT = ".\\src\\resources\\fonts\\arial.ttf";
    public static String inputFilepath = "C:\\Projects\\my\\PdfPageNumeration\\test-pdf.pdf";
    public static String outputFilepath = "C:\\Projects\\my\\PdfPageNumeration\\test-pdf-out.pdf";
    public static String pageNumberPattern = "Страница %d из %d";

    public static void main(String[] args) throws IOException, DocumentException {

        PdfReader reader = new PdfReader(inputFilepath);
        PdfStamper stamper = new PdfStamper(reader,
                new FileOutputStream(outputFilepath));

        BaseFont bf=BaseFont.createFont(FONT, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        StringBuilder text = new StringBuilder();
        float posY = 15;
        float posX = 0;

        for (int i=1; i <= reader.getNumberOfPages(); i++){
            text.append(String.format(pageNumberPattern, i, reader.getNumberOfPages()));
            if(reader.getPageSize(i).getHeight() > 0 && reader.getPageSize(i).getWidth() > 0) {
                posX = reader.getPageSize(i).getWidth() / 2 - text.length();
            }
            PdfContentByte over = stamper.getOverContent(i);

            over.beginText();
            over.setFontAndSize(bf, 9);
            over.setTextMatrix(posX, posY);
            over.showText(text.toString());
            over.endText();

            text.setLength(0);
        }
        stamper.close();
    }

}
