package testing.buttons;

import arc.graphics.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import testing.ui.*;
import testing.util.*;

import static mindustry.Vars.*;

public class LightSwitch{
    public static void lightButton(Table t){
        Cell<ImageButton> i = t.button(TUIcons.lightOff, TUStyles.tuImageStyle, TUVars.iconSize, () -> {
            if(TUVars.pressTimer > TUVars.longPress) return;
            state.rules.lighting = !state.rules.lighting;
        });

        ImageButton b = i.get();

        TUElements.boxTooltip(b, "@tu-tooltip.button-light");
        b.getStyle().imageChecked = TUIcons.lightOn;
        b.getStyle().imageCheckedColor = new Color();

        b.update(() -> {
            if(b.isPressed() && !b.isDisabled() && !net.client()){
                TUVars.pressTimer += TUVars.delta();
                if(TUVars.pressTimer > TUVars.longPress){
                    ui.picker.show(state.rules.ambientLight, true, res -> state.rules.ambientLight.set(res));
                }
            }

            b.setChecked(state.rules.lighting);
            if(b.isChecked()){
                b.getStyle().imageCheckedColor.set(state.rules.ambientLight).a(1);
            }
        });
        b.released(() -> TUVars.pressTimer = 0);
    }
}
