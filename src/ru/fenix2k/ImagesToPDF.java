package ru.fenix2k;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.apache.tika.Tika;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Нумерует файлы изображения, PDF согласно шаблону.
 * Конвертирует файлы изображения (jpg, jpeg, gif, png) в PDF.
 * Итоговый файл в формате PDF.
 */
public class ImagesToPDF {
    private static final String sourceDir = ".\\sourceDir";
    private static final String outputDocument = ".\\print.pdf";

    /** Пусть к используемому шрифту к поддержкой киррилицы */
    private static final String FONT = "C:\\Windows\\Fonts\\arial.ttf";
    /** Шаблон строки нумерации, например "Страница %d из %d" */
    private static final String PAGE_NUMBER_PATTERN = "Страница %d";
    /** Префикс временных файлов */
    private static final String TMP_FILE_PREFIX = "+~";

    public static void main(String[] args) {
        ImagesToPDF imagesToPDF = new ImagesToPDF();
        try {
            //Path output = Paths.get(outputDocument);
            File[] files = imagesToPDF.getDirectoryFiles(sourceDir);
            List<String> filesList = Arrays.stream(files)
                    .map(File::toString)
                    .collect(Collectors.toList());

            // объединение и нумерация pdf
            String outputFilepath = imagesToPDF.mergeAndNumberingPdf(filesList, "");

            System.out.println("Result= " + new File(outputFilepath).getCanonicalFile());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Выполняет конвертацию в PDF изображений, объединение и нумерацию PDF
     * @param files список исходных файлов
     * @param tempFolder путь к временной папке
     * @return итоговый файл
     * @throws Exception
     */
    public String mergeAndNumberingPdf(List<String> files, String tempFolder) throws Exception {
        try {
            List<String> tmpfilesList = new ArrayList<>();
            for (String filepath : files) {
                File file = new File(filepath);
                if(!file.getName().startsWith(TMP_FILE_PREFIX))
                    tmpfilesList.add(convertToPDF(file));
            }
            Path output = Paths.get(tempFolder, "print-" + generateRandomString(20) + ".pdf");
            Path outputFilepath = createPageNumberingPdfFromDocList(tmpfilesList, output, PAGE_NUMBER_PATTERN, FONT);
            // удаление временных файлов
            for (String filename : tmpfilesList)
                Files.delete(Paths.get(filename));

            return outputFilepath.toString();
        } catch (Exception ex) {
            throw new Exception("Не удалось сформировать документ для печати", ex);
        }
    }

    /**
     * Конвертирует файл изображения в PDF и возвращает путь к файлу
     * @param file исходный фалй
     * @return путь к файлу
     * @throws Exception
     */
    private String convertToPDF(File file) throws Exception {
        // опредение типа файла (MIMETYPE)
        String mimeType = getFileMimeType(file);
        if(mimeType == null || mimeType.length() == 0) {
            System.out.println("MimeType is missing: " + file.getPath());
        }

        String filePath = null;
        // Если тип pdf, то конвертацию не делаем
        // если изображение, то конвертируем в pdf
        // иначе ошибка
        if(mimeType.equals("application/pdf")) {
            filePath = file.getPath();
        }
        else if(mimeType.equals("image/jpeg")
                || mimeType.equals("image/png")
                || mimeType.equals("image/tiff")
                || mimeType.equals("image/gif")) {
            filePath = imageToPdf(file);
        }
        else {
            System.out.println("MimeType is not supported: " + file.getPath());
        }
        return filePath;
    }

    /**
     * Объеденяет и нумерует несколько pdf файлов
     * @param files список файлов
     * @param output путь к итоговому файлу
     * @param pageNumberPattern Шаблон строки нумерации, например "Страница %d"
     * @param font Пусть к используемому шрифту к поддержкой киррилицы
     * @return путь к итоговому файлу
     * @throws Exception
     */
    private Path createPageNumberingPdfFromDocList(List<String> files, Path output, String pageNumberPattern, String font)
            throws Exception {
        com.itextpdf.text.Document document = new com.itextpdf.text.Document();
        PdfCopy copy = new PdfCopy(document, new FileOutputStream(output.toString()));
        PdfCopy.PageStamp stamp;
        document.open();
        int n;
        int pageNo = 0;
        PdfImportedPage page;
        BaseFont bf = BaseFont.createFont(font, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        // позиция выводимой строки
        float posY = 15;
        float posX = 0;
        for (String file : files) {
            PdfReader reader = new PdfReader(new FileInputStream(file));
            n = reader.getNumberOfPages();
            for (int i = 0; i < n; ) {
                pageNo++;
                page = copy.getImportedPage(reader, ++i);
                stamp = copy.createPageStamp(page);

                // добавлние текста
                if(reader.getPageSize(i).getHeight() > 0 && reader.getPageSize(i).getWidth() > 0)
                    posX = reader.getPageSize(i).getWidth() / 2;

                PdfContentByte over = stamp.getOverContent();
                over.beginText();
                over.setFontAndSize(bf, 9);
                over.showTextAligned(
                        PdfContentByte.ALIGN_CENTER,
                        String.format(pageNumberPattern, pageNo),
                        posX, posY, 0);
                over.endText();
                stamp.alterContents();

                copy.addPage(page);
            }
            reader.close();
        }
        document.close();
        return output;
    }

    /**
     * Возвращает список файлов в указанной папке
     * @param dirPath исходная папка
     * @return масив фалйов
     */
    private File[] getDirectoryFiles(String dirPath) {
        File dir = new File(dirPath);
        return dir.listFiles();
    }

    /**
     * Генерирует случайную строку
     * @return строка символов
     */
    public String generateRandomString(int length) {
        int targetStringLength = 10;
        if(length != 0)
            targetStringLength = length;

        String alphaNumericString = "abcdefghijklmnopqrstuvxyz"
                + "0123456789";
        StringBuilder line = new StringBuilder();
        ThreadLocalRandom.current()
                .ints(0, alphaNumericString.length())
                .limit(targetStringLength)
                .forEach(num -> line.append(alphaNumericString.charAt(num)));
        return line.toString().toUpperCase();
    }

    /**
     * Получает фактический формат файла MIME TYPE
     * @param file исходный файл
     * @return MIME TYPE
     * @throws IOException
     */
    private String getFileMimeType(File file) throws IOException {
        Tika tika = new Tika();
        return tika.detect(file);
    }

    /**
     * Конвертирует изображения в pdf.
     * Поддерживаются форматы изображения jpg, jpeg, gif, png.
     * @param file исходный файл изображения
     * @return путь к созданному pdf
     * @throws Exception
     */
    private String imageToPdf(File file) throws Exception {
        // генерация имени итогового файла
        String path = file.getParent();
        String filename = file.getName();
        String name;
        int dotIndex = filename.lastIndexOf(".");
        if(dotIndex != -1) name = filename.substring(0, dotIndex);
        else name = filename;

        String output = Paths.get(path, TMP_FILE_PREFIX + name + "-" + generateRandomString(10) + ".pdf").toString();

        // конвертация изображения в PDF
        try (FileOutputStream fos = new FileOutputStream(output)) {
            Image image = Image.getInstance(file.getPath());
            // определяем формат изображения А4 (вертикально или горизонтально)
            Rectangle pageSize = new Rectangle(PageSize.A4.getWidth(), PageSize.A4.getHeight());
            if(image.getPlainWidth() > PageSize.A4.getWidth()
                    && image.getPlainWidth() > image.getPlainHeight())
                pageSize = new Rectangle(PageSize.A4.getHeight(), PageSize.A4.getWidth());

            int margin = 30;
            Document document = new Document(pageSize, margin, margin, margin, margin);
            PdfWriter writer = PdfWriter.getInstance(document, fos);
            writer.open();
            document.open();

            // масштабирование к формату A4
            float xs = (pageSize.getWidth() - margin)/image.getPlainWidth() * 100 - 2;
            float ys = (pageSize.getHeight() - margin)/image.getPlainHeight() * 100 - 4;
            if(xs > ys)
                image.scalePercent(ys);
            else
                image.scalePercent(xs);
            document.add(image);

            document.close();
            writer.close();
        } catch (Exception ex) {
            throw new Exception("Ошибка конвертации файла изображения в pdf", ex);
        }
        return output;
    }

}
