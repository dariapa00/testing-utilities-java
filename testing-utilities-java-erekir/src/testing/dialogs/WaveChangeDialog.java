package testing.dialogs;

import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.TextField.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import blui.*;
import blui.ui.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import testing.util.*;

import static arc.Core.*;
import static mindustry.Vars.*;

public class WaveChangeDialog extends TUBaseDialog{
    private final Table all = new Table();
    private static int amountCounter;
    private int startWave = 1, waves = 50;

    public WaveChangeDialog(){
        super("@tu-unit-menu.waves");

        all.margin(20).marginTop(0f);

        cont.add("@tu-unit-menu.wave-range").right();
        cont.table(w -> {
            w.add("@tu-unit-menu.wave-start").left();
            TextField minField = BLElements.textField(
                String.valueOf(startWave),
                text -> {
                    startWave = Math.max(Strings.parseInt(text), 1);
                    rebuild();
                },
                () -> String.valueOf(startWave),
                TextFieldFilter.digitsOnly,
                Strings::canParsePositiveInt
            );
            w.add(minField).left().padLeft(6).width(BLVars.fieldWidth);
            w.row();

            w.add("@tu-unit-menu.wave-waves").left();
            TextField maxField = BLElements.textField(
                String.valueOf(waves),
                text -> {
                    waves = Math.max(Strings.parseInt(text), 1);
                    rebuild();
                },
                () -> String.valueOf(waves),
                TextFieldFilter.digitsOnly,
                Strings::canParsePositiveInt
            );
            w.add(maxField).left().padLeft(6).width(BLVars.fieldWidth);
        }).left();
        cont.row();
        cont.label(() -> bundle.format("tu-unit-menu.wave-current", state.wave)).colspan(2);
        cont.row();
        cont.pane(all).colspan(2);

        BLElements.boxTooltip(
            buttons.button("@tu-unit-menu.wave-send", Icon.upload, this::sendWave).get(),
            "@tu-tooltip.unit-send-wave"
        );
    }

    @Override
    protected void rebuild(){
        title.setText(bundle.format("tu-unit-menu.waves-menu", state.map.name()));

        all.clear();

        float iconMul = 2f;
        all.table(t -> {
            for(int i = startWave; i < startWave + waves; i++){
                int ii = i;
                TextButton b = t.button(String.valueOf(i), () -> setWave(ii)).right().grow().padRight(6f).padTop(4f).get();
                b.getLabel().setWrap(false);
                b.getLabelCell().center();
                b.setDisabled(() -> net.client());

                amountCounter = 0;
                t.table(w -> {
                    int wave = ii - 1;
                    for(SpawnGroup group : state.rules.spawns){
                        if(group.getSpawned(wave) <= 0) continue;
                        w.table(u -> {
                            int a = group.getSpawned(wave) * Utils.countSpawns(group);
                            u.add(BLElements.itemImage(
                                new TextureRegionDrawable(group.type.uiIcon),
                                () -> String.valueOf(a)
                            )).size(8 * 4 * iconMul).top().grow();
                            amountCounter += a;
                            boolean hasEffect = group.effect != null && group.effect != StatusEffects.none,
                                hasShield = group.getShield(wave) > 0;
                            if(hasEffect || hasShield){
                                u.row();
                                u.table(e -> {
                                    if(hasEffect){
                                        e.add(new Image(group.effect.uiIcon).setScaling(Scaling.fit)).size(8 * 2 * iconMul);
                                    }
                                    if(hasShield){
                                        e.add(BLElements.itemImage(
                                            Icon.defense,
                                            () -> Utils.round(group.getShield(wave)),
                                            Pal.accentBack,
                                            Pal.accent,
                                            1f,
                                            Align.center
                                        )).size(8 * 2 * iconMul).growX();
                                    }
                                }).top().grow();
                            }
                        }).grow();
                    }
                }).growY().left().padTop(4f);
                t.label(() -> bundle.format("tu-unit-menu.wave-total", amountCounter)).grow().left().padLeft(6f).padTop(4f);
                t.row();
            }
        });
    }

    void sendWave(){
        if(net.client()){
            Utils.runCommand("runwave");
        }else{
            if(input.shift()){
                Utils.copyJS("Vars.logic.runWave();");
                return;
            }

            logic.runWave();
        }
    }

    void setWave(int wave){
        if(input.shift()){
            Utils.copyJS("Vars.state.wave = @;", wave);
            return;
        }

        state.wave = wave;
    }
}
