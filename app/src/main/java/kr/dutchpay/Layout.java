package kr.dutchpay;

import com.google.mlkit.vision.text.Text;
import android.graphics.Point;
import android.graphics.Rect;
import java.util.*;

public final class Layout {
    public static List<String> rows(Text text) {
        List<Text.Line> lines = new ArrayList<>();
        List<Double> slopes = new ArrayList<>();
        for (Text.TextBlock b : text.getTextBlocks()) for (Text.Line l : b.getLines()) {
            if (l.getBoundingBox() == null) continue;
            lines.add(l);
            Point[] p = l.getCornerPoints();
            if (p != null && p[1].x - p[0].x > 80)
                slopes.add((double)(p[1].y - p[0].y) / (p[1].x - p[0].x));
        }
        Collections.sort(slopes);
        double slope = slopes.isEmpty() ? 0 : slopes.get(slopes.size()/2);
        lines.sort(Comparator.comparingDouble(l -> y(l, slope)));
        List<List<Text.Line>> groups = new ArrayList<>();
        for (Text.Line l : lines) {
            List<Text.Line> last = groups.isEmpty() ? null : groups.get(groups.size()-1);
            if (last == null || Math.abs(y(l, slope) - y(last.get(0), slope))
                    > Math.min(l.getBoundingBox().height(), last.get(0).getBoundingBox().height()) * .55) {
                last = new ArrayList<>(); groups.add(last);
            }
            last.add(l);
        }
        List<String> result = new ArrayList<>();
        for (List<Text.Line> group : groups) {
            group.sort(Comparator.comparingInt(l -> l.getBoundingBox().left));
            List<String> parts = new ArrayList<>();
            for (Text.Line l : group) parts.add(l.getText());
            result.add(String.join("\t", parts));
        }
        return result;
    }
    private static double y(Text.Line line, double slope) {
        Rect r = line.getBoundingBox();
        return r.exactCenterY() - slope * r.exactCenterX();
    }
}
