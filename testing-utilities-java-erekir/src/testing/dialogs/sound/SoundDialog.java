package testing.dialogs.sound;

import arc.*;
import arc.audio.*;
import arc.graphics.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import blui.*;
import blui.ui.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import testing.dialogs.*;
import testing.util.*;

import static arc.Core.*;
import static mindustry.Vars.*;

public class SoundDialog extends TUBaseDialog{
    /** Audio bus for sounds played by the dialog. Will remain unpaused unlike other audio busses. */
    private static final AudioBus soundRoomBus = new AudioBus();

    private FilterTable filters = null;
    private SoundsTable soundsTable;
    private MusicsTable musicsTable;
    private STable current;
    private Table all;
    private TextField search;

    public SoundDialog(){
        super("@tu-sound-menu.name");

        Core.app.post(this::build); //Allow mods to load their sounds
    }

    private void build(){
        LoadedSounds.init();

        soundsTable = new SoundsTable(soundRoomBus);
        current = soundsTable;
        musicsTable = new MusicsTable();

        cont.table(s -> {
            if(settings.getBool("tu-music-enabled", false)){
                ImageButton ib = new ImageButton(TUIcons.sounds);
                ib.changed(() -> {
                    current.stopSounds();
                    if(current == soundsTable){
                        current = musicsTable;
                        ib.replaceImage(new Image(TUIcons.musics).setScaling(Scaling.fit));
                    }else{
                        current = soundsTable;
                        ib.replaceImage(new Image(TUIcons.sounds).setScaling(Scaling.fit));
                    }
                    makeUI();
                });
                ib.label(() -> current == soundsTable ? "@tu-sound-menu.sound" : "@tu-sound-menu.music").wrapLabel(false).left().padRight(8);
                s.add(ib).height(BLVars.iconSize);
            }
            s.image(Icon.zoom).padRight(8).padLeft(8);
            search = s.field(null, text -> rebuild()).growX().get();
            search.setMessageText("@players.search");
        }).fillX().padBottom(4).row();

        cont.add(all = new Table()).grow().top();
        makeUI();

        shown(() -> {
            //Pause the ui audio bus while open so that button press sounds doesn't play.
            audio.setPaused(Sounds.press.bus.id, true);
            if(filters != null) filters.shown();
        });
        hidden(() -> {
            soundsTable.stopSounds();
            musicsTable.stopSounds();
            if(filters != null) TUFilters.closed();
            audio.setPaused(Sounds.press.bus.id, false);
        });

        if(settings.getBool("tu-music-enabled", false)){
            update(() -> {
                if(isShown()){
                    if(current == musicsTable){
                        musicsTable.update();
                    }else{
                        Reflect.set(control.sound, "fade", 0f);
                        Reflect.invoke(control.sound, "silence");
                        if(soundControlPlaying() != null) Reflect.invoke(control.sound, "silence"); //Counteract fade in
                    }
                }
            });
        }
    }

    private void makeUI(){
        all.clear();
        all.defaults().padLeft(32).padRight(32).center().top();

        all.table(sel -> {
            sel.defaults().growX();
            current.createSelection(sel, search);
        }).grow().row();

        BLElements.divider(all, null, Color.lightGray);

        all.pane(t -> {
            t.defaults().top();
            current.createPlay(t);
            t.row();

            if(!Core.settings.getBool("tu-allow-filters", false)) return;
            BLElements.divider(t, "Audio Filters", Pal.accent);
            t.table(fil -> {
                if(filters == null) filters = new FilterTable();
                fil.add(filters);
            }).center();
        }).grow().minHeight(180);
    }

    @Override
    protected void rebuild(){
        current.rebuild();
    }

    /** What SoundControl is currently running play() on. Used to check if it needs to be counteracted.  */
    protected static Music soundControlPlaying(){
        if(state.isMenu()){
            if(ui.planet.isShown()){
                return Musics.launch;
            }else if(ui.editor.isShown()){
                return Musics.editor;
            }else{
                return Musics.menu;
            }
        }else if(state.rules.editor){
            return Musics.editor;
        }
        return null;
    }
}
