package testing.buttons;

import arc.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import blui.scene.ui.*;
import blui.ui.*;
import mindustry.game.*;
import mindustry.gen.*;
import testing.ui.*;
import testing.util.*;

import static mindustry.Vars.*;
import static testing.ui.TUDialogs.*;

public class TeamChanger{
    public static void changeTeam(Team team){
        if(net.client()){ //For 2r2t
            Utils.runCommand("team @", team.id);
        }else{
            if(Core.input.shift()){
                Utils.copyJS("Vars.player.team(Team.get(@));", team.id);
                return;
            }

            player.team(team);
        }
    }

    public static Cell<Table> teamChanger(Table t){
        return t.table(teams -> {
            BLElements.boxTooltip(teams, "@tu-tooltip.button-team");
            int i = 0;
            for(Team team : Team.baseTeams){
                HoldImageButton button = new HoldImageButton(Tex.whiteui, TUStyles.clearNoneTogglehi);
                button.clicked(() -> changeTeam(team));
                button.held(() -> teamDialog.show(curTeam(), TeamChanger::changeTeam));

                button.getImageCell().grow().scaling(Scaling.stretch).center().pad(0).margin(0);
                button.getStyle().imageUpColor = team.color;
                button.update(() -> button.setChecked(player.team() == team));

                teams.add(button).grow().margin(6f).center();
                if(++i % 3 == 0){
                    teams.row();
                }
            }
        });
    }

    public static Team curTeam(){
        return player.team();
    }

    public static Team getNextTeam(){
        if(player.team() == state.rules.defaultTeam){
            return state.rules.waveTeam;
        }else{
            return state.rules.defaultTeam;
        }
    }

    public static void addButton(Table t){
        teamChanger(t).width(100);
    }

    static String teamName(){
        String t = teamDialog.teamName(curTeam());
        return "[#" + curTeam().color.toString() + "]" + t.substring(0, 1).toUpperCase() + t.substring(1);
    }
}
