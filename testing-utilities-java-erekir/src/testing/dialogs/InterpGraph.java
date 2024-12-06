package testing.dialogs;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.pooling.*;
import mindustry.gen.*;
import mindustry.ui.*;
import testing.util.*;

public class InterpGraph extends Table{
    private int points;
    private Interp oldInterp = Interp.linear;
    private Interp interp = Interp.linear;
    private float oldMinVal = 0f, oldMaxVal = 1f,
        minVal = 0f, maxVal = 1f;
    private float lerp = 1;

    public InterpGraph(){
        background(Tex.pane);

        float dotColumnWidth = 40f;
        rect((x, y, width, height) -> {
            Lines.stroke(Scl.scl(3f));

            GlyphLayout lay = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
            Font font = Fonts.outline;

            lay.setText(font, "-0.00");

            boolean interpColumn = width > 300 + dotColumnWidth;
            float min = min(), max = max(), range = max - min;
            float offsetX = Scl.scl(lay.width + 6f), offsetY = Scl.scl(5f);

            float graphX = x + offsetX, graphW = width - offsetX;
            float baseY = y + offsetY, baseH = height - offsetY;
            float graphY = baseY + baseH * (-min / range), graphH = baseH - baseH * ((-min + (max - 1)) / range);

            if(interpColumn) graphW -= dotColumnWidth;
            float colCenter = x + width - dotColumnWidth / 2f;

            points = Mathf.round(graphW / 4, 2) + 1; //Ensure a center (0.5) point
            float spacing = graphW / (points - 1);

            float tickMin = graphY;
            while(tickMin - (graphH / 4) > baseY){
                tickMin -= graphH / 4;
            }

            Draw.color(Color.darkGray);
            float gTick = tickMin;
            while(gTick < baseY + baseH){
                if(gTick != graphY && gTick != graphY + graphH) Lines.line(graphX, gTick, graphX + graphW, gTick);
                gTick += graphH / 4;
            }

            Draw.color(Color.lightGray);
            Lines.line(graphX, graphY, graphX + graphW, graphY);
            Lines.line(graphX, graphY + graphH, graphX + graphW, graphY + graphH);

            if(range != 1){
                Lines.line(graphX, baseY, graphX + graphW, baseY);
                Lines.line(graphX, baseY + baseH, graphX + graphW, baseY + baseH);
            }

            if(interpColumn){
                Draw.color(Color.darkGray);
                float colTick = tickMin;
                while(colTick < baseY + baseH){
                    if(colTick != graphY && colTick != graphY + graphH) Lines.lineAngleCenter(colCenter, colTick, 0, dotColumnWidth / 3f, false);
                    colTick += graphH / 4;
                }

                Draw.color(Color.lightGray);
                Lines.lineAngleCenter(colCenter, graphY, 0, dotColumnWidth / 3f, false);
                Lines.lineAngleCenter(colCenter, graphY + graphH, 0, dotColumnWidth / 3f, false);

                if(range != 1){
                    Lines.lineAngleCenter(colCenter, baseY, 0, dotColumnWidth / 3f, false);
                    Lines.lineAngleCenter(colCenter, baseY + baseH, 0, dotColumnWidth / 3f, false);
                }
            }

            Draw.color(Color.red);
            Lines.beginLine();
            for(int i = 0; i < points; i++){
                float a = i / (points - 1f);
                float cx = graphX + i * spacing, cy = graphY + applyInterp(a) * graphH;
                Lines.linePoint(cx, cy);
            }
            Lines.endLine();

            float a = Time.globalTime % 180f / 180f;
            Fill.circle(graphX + graphW * a, graphY + applyInterp(a) * graphH, 4f);

            if(interpColumn){
                Fill.circle(colCenter, graphY + applyInterp(a) * graphH, 4f);
            }

            lay.setText(font, "0.00");
            font.draw("0.00", graphX, graphY + lay.height / 2f, Align.right);
            lay.setText(font, "1.00");
            font.draw("1.00", graphX, graphY + graphH + lay.height / 2f, Align.right);

            if(range != 1){
                String s = Strings.fixed(min, 2);
                lay.setText(font, s);
                font.draw(s, graphX, baseY + lay.height / 2f, Align.right);
                s = Strings.fixed(max, 2);
                lay.setText(font, s);
                font.draw(s, graphX, baseY + baseH + lay.height / 2f, Align.right);
            }

            font.setColor(Color.white);
            Pools.free(lay);

            Draw.reset();
        }).pad(4).padBottom(10).grow();

        update(() -> {
            if(lerp < 1){
                float t = Core.settings.getInt("tu-lerp-time") / 4f * 60f;
                if(t <= 0){
                    lerp = 1;
                }else{
                    lerp = Mathf.clamp(lerp + (TUVars.delta()) / t);
                }
            }
        });
    }

    float applyInterp(float a){
        if(lerp >= 1){
            return interp.apply(a);
        }
        return Mathf.lerp(oldInterp.apply(a), interp.apply(a), lerp());
    }

    float min(){
        if(lerp >= 1){
            return minVal;
        }
        return Mathf.lerp(oldMinVal, minVal, lerp());
    }

    float max(){
        if(lerp >= 1){
            return maxVal;
        }
        return Mathf.lerp(oldMaxVal, maxVal, lerp());
    }

    float lerp(){
        return Interp.smoother.apply(lerp);
    }

    public void setInterp(Interp newInterp){
        if(lerp < 1){
            Interp o = oldInterp;
            Interp i = interp;
            float l = Interp.smoother.apply(lerp);

            oldInterp = a -> Mathf.lerp(o.apply(a), i.apply(a), l);

            oldMinVal = min();
            oldMaxVal = max();
        }else{
            oldInterp = interp;
            oldMinVal = minVal;
            oldMaxVal = maxVal;
        }

        interp = newInterp;
        lerp = 0;

        minVal = Float.MAX_VALUE;
        maxVal = Float.MIN_VALUE;
        for(int i = 0; i < points; i++){
            float v = newInterp.apply(i / (points - 1f));
            if(v < minVal){
                minVal = v;
            }
            if(v > maxVal){
                maxVal = v;
            }
        }
    }
}
