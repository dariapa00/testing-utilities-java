package testing.dialogs.world;

import arc.math.*;
import arc.scene.ui.*;
import arc.scene.ui.TextField.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import blui.*;
import blui.ui.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.type.*;
import testing.ui.*;
import testing.util.*;

import static arc.Core.*;
import static mindustry.Vars.*;

public class WeatherTable extends Table{
    private final Table selection = new Table();
    private final float minDur = 0.125f, maxDur = 600f;
    private TextField search;
    private Weather weather = Weathers.rain;
    private float intensity = 100f, duration = 60f;

    public WeatherTable(){
        table(s -> {
            s.image(Icon.zoom).padRight(8);
            search = s.field(null, text -> rebuild()).growX().get();
            search.setMessageText("@players.search");
        }).fillX().padBottom(4).row();

        pane(all -> all.add(selection)).row();

        table(set -> {
            set.table(s -> {
                BLElements.sliderSet(
                    s, text -> intensity = Mathf.clamp(Strings.parseFloat(text), 0f, 100f), () -> String.valueOf(intensity),
                    TextFieldFilter.floatsOnly, Strings::canParsePositiveFloat,
                    0f, 100f, 1f, intensity, (n, f) -> {
                        intensity = Mathf.clamp(n, 0f, 100f);
                        f.setText(String.valueOf(intensity));
                    },
                    "@tu-weather-menu.intensity",
                    "@tu-tooltip.weather-intensity"
                );
                s.row();

                BLElements.sliderSet(
                    s, text -> duration = Strings.parseFloat(text), () -> String.valueOf(duration),
                    TextFieldFilter.floatsOnly, Strings::canParsePositiveFloat,
                    minDur, maxDur, 0.125f, duration, (n, f) -> {
                        duration = n;
                        f.setText(String.valueOf(n));
                    },
                    "@tu-status-menu.duration",
                    "@tu-tooltip.weather-duration"
                );
            });
            set.row();

            ImageButton wb = set.button(TUIcons.get(Icon.add), BLVars.buttonSize, this::createWeather).get();
            BLElements.boxTooltip(wb, "@tu-tooltip.weather-create");
            wb.label(() -> "@tu-weather-menu.create").padLeft(6).growX();
            wb.setDisabled(() -> intensity <= 0 || duration <= 0);
            set.row();

            set.table(b -> {
                ImageButton rb = b.button(TUIcons.get(Icon.cancel), TUStyles.lefti, BLVars.buttonSize, this::removeWeather).get();
                BLElements.boxTooltip(rb, "@tu-tooltip.weather-remove");
                rb.label(() -> "@tu-weather-menu.remove").padLeft(6).growX();

                ImageButton cb = b.button(TUIcons.get(Icon.trash), TUStyles.righti, BLVars.buttonSize, this::clearWeather).get();
                cb.label(() -> "@tu-weather-menu.clear").padLeft(6).growX();
            });
        }).padTop(6f);
    }

    protected void rebuild(){
        selection.clear();
        String text = search.getText();

        selection.label(
            () -> bundle.get("tu-menu.selection") + weather.localizedName
        ).padBottom(6);
        selection.row();

        Seq<Weather> array = content.<Weather>getBy(ContentType.weather).select(w -> w.localizedName.toLowerCase().contains(text.toLowerCase()));
        selection.table(list -> {
            list.left().defaults().minWidth(250);

            int cols = 3;
            int count = 0;

            for(Weather w : array){
                TextButton button = list.button(w.localizedName, () -> weather = w).uniform().grow().get();
                //button.getLabel().setWrap(false);
                if(w.fullIcon.found()){
                    button.add(new Image(w.fullIcon));
                    button.getCells().reverse();
                }

                if((++count) % cols == 0){
                    list.row();
                }
            }
        }).growX().left().padBottom(10);
        selection.row();
    }

    private void createWeather(){
        if(input.shift()){
            Utils.copyJS("Vars.content.getByID(ContentType.weather, @).create(@, @);",
                weather.id, intensity / 100f, duration * 60f
            );
            return;
        }

        weather.create(intensity / 100f, duration * 60f);
    }

    private void removeWeather(){
        if(input.shift()){
            Utils.copyJS("Groups.weather.each(w => w.weather == weather, w => w.remove());");
            return;
        }

        Groups.weather.each(w -> w.weather == weather, WeatherState::remove);
    }

    private void clearWeather(){
        Groups.weather.each(WeatherState::remove);
    }
}
