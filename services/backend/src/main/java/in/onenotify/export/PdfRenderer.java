package in.onenotify.export;

import java.io.*;
import java.util.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.springframework.stereotype.Component;

@Component
public class PdfRenderer {
  public byte[] render(String title, List<String> lines) {
    try (var doc = new PDDocument();
        var out = new ByteArrayOutputStream()) {
      var font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
      var bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
      PDPageContentStream stream = null;
      float y = 0;
      var all = new ArrayList<String>();
      all.add(title);
      all.add("OneNotify | Private family case report | " + java.time.LocalDate.now());
      all.add("");
      all.addAll(lines);
      for (String raw : all) {
        String safe = raw.replaceAll("[^\\x20-\\x7E]", "?");
        for (int i = 0; i < Math.max(1, safe.length()); i += 88) {
          if (stream == null || y < 55) {
            if (stream != null) stream.close();
            var page = new PDPage();
            doc.addPage(page);
            stream = new PDPageContentStream(doc, page);
            y = 740;
          }
          stream.beginText();
          stream.setFont(raw.equals(title) ? bold : font, raw.equals(title) ? 18 : 10);
          stream.newLineAtOffset(45, y);
          stream.showText(safe.substring(i, Math.min(i + 88, safe.length())));
          stream.endText();
          y -= 17;
        }
      }
      if (stream != null) stream.close();
      doc.save(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException("Report generation failed", e);
    }
  }
}
