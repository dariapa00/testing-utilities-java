package testing.dialogs;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import blui.*;
import blui.ui.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.legacy.*;
import testing.*;
import testing.buttons.*;
import testing.ui.*;
import testing.util.*;

import static arc.Core.*;
import static mindustry.Vars.*;
import static testing.ui.TUDialogs.*;

public class BlockDialog extends TUBaseDialog{
    private final Table selection = new Table();
    private TextField search;
    private Block block = Blocks.coreShard;
    private Team placeTeam = Team.get(settings.getInt("tu-default-team", 1));
    private int placePos = -1, rotation = 1;
    private boolean expectingPos, initialized;

    public BlockDialog(){
        super("@tu-block-menu.name");

        cont.table(s -> {
            s.image(Icon.zoom).padRight(8);
            search = s.field(null, text -> rebuild()).growX().get();
            search.setMessageText("@players.search");
        }).fillX().padBottom(4).row();

        cont.label(() -> bundle.get("tu-menu.selection") + block.localizedName).padBottom(6).row();

        cont.pane(all -> all.add(selection)).fillX().row();

        cont.table(t -> {
            BLElements.imageButton(
                t, TUIcons.get(Icon.defense), TUStyles.lefti, BLVars.buttonSize,
                () -> teamDialog.show(placeTeam, team -> placeTeam = team),
                () -> bundle.format("tu-unit-menu.set-team", "[#" + placeTeam.color + "]" + teamName() + "[]"),
                "@tu-tooltip.block-set-team"
            );

            BLElements.imageButton(
                t, TUIcons.get(Icon.map), TUStyles.toggleRighti, BLVars.buttonSize,
                () -> {
                    hide();
                    expectingPos = true;
                },
                () -> bundle.format("tu-unit-menu.pos", Point2.x(placePos), Point2.y(placePos)),
                "@tu-tooltip.block-pos"
            );
        }).padTop(6).row();

        cont.table(p -> {
            ImageButton rb = BLElements.imageButton(
                p, TUIcons.get(Icon.up), TUStyles.lefti, BLVars.buttonSize,
                () -> rotation = Mathf.mod(rotation - 1, 4),
                null, "@tu-tooltip.block-rotate"
            ).get();
            rb.setDisabled(() -> !block.rotate);
            rb.update(() -> ((TextureRegionDrawable)(rb.getStyle().imageUp)).setRegion(getDirection()));

            ImageButton pb = BLElements.imageButton(
                p, new TextureRegionDrawable(block.uiIcon), TUStyles.centeri, BLVars.buttonSize,
                this::placeBlock,
                () -> "@tu-block-menu.place",
                "@tu-tooltip.block-place"
            ).get();
            pb.setDisabled(() -> world.tile(placePos) == null);
            pb.update(() -> {
                ((TextureRegionDrawable)(pb.getStyle().imageUp)).setRegion(block.uiIcon);
            });

            ImageButton cb = BLElements.imageButton(
                p, TUIcons.get(Icon.cancel), TUStyles.righti, BLVars.buttonSize,
                this::deleteBlock,
                () -> "@tu-block-menu.delete",
                "@tu-tooltip.block-delete"
            ).get();
        }).padTop(6f).row();

        ImageButton pb = BLElements.imageButton(
            cont, TUIcons.get(Icon.terrain), Styles.defaulti, BLVars.buttonSize,
            () -> {
                Setup.terrainFrag.show();
                hide();
            },
            () -> "@tu-block-menu.open-painter",
            "@tu-tooltip.block-terrain-painter-open"
        ).get();
        pb.setDisabled(() -> net.client() || Setup.terrainFrag.shown());

        if(!initialized){
            Events.on(WorldLoadEndEvent.class, e -> {
                placePos = Point2.pack(world.width() / 2, world.height() / 2);
            });

            Events.run(Trigger.update, () -> {
                if(expectingPos){
                    if(!state.isGame()){
                        expectingPos = false;
                    }else if(TestUtils.click()){
                        if(!Utils.hasMouse()){
                            int x = World.toTile(input.mouseWorldX()),
                                y = World.toTile(input.mouseWorldY());
                            placePos = Point2.pack(x, y);
                            ui.showInfoToast(bundle.format("tu-unit-menu.set-pos", x, y), 4f);
                            show();
                        }else{
                            ui.showInfoToast("@tu-unit-menu.cancel", 4f);
                        }
                        expectingPos = false;
                    }
                }
            });
            initialized = true;
        }
    }

    public void drawPos(){
        if(net.client()) return;
        float size = block.size * tilesize,
            offset = (1 - block.size % 2) * tilesize / 2f,
            x, y;
        if(expectingPos && state.isGame() && !Utils.hasMouse()){
            x = World.toTile(input.mouseWorldX()) * tilesize;
            y = World.toTile(input.mouseWorldY()) * tilesize;
        }else if(Spawn.blockHover){
            x = Point2.x(placePos) * tilesize;
            y = Point2.y(placePos) * tilesize;
        }else{
            return;
        }
        Draw.z(Layer.overlayUI);
        Lines.stroke(1f, placeTeam.color);
        Lines.rect(x - size / 2 + offset, y - size / 2 + offset, size, size);
        Draw.rect(Icon.cancel.getRegion(), x, y, tilesize / 2f, tilesize / 2f);
    }

    @Override
    protected void rebuild(){
        expectingPos = false;
        selection.clear();
        String text = search.getText();

        Seq<Block> array = content.blocks()
            .select(b -> !b.isFloor() && !b.isStatic() &&
                !(b instanceof Prop) &&
                !(b instanceof TallBlock) &&
                !(b instanceof TreeBlock) &&
                !(b instanceof ConstructBlock) &&
                !(b instanceof LegacyBlock) &&
                (!b.isHidden() || settings.getBool("tu-show-hidden")) &&
                (text.isEmpty() || b.localizedName.toLowerCase().contains(text.toLowerCase())));
        if(array.size == 0) return;

        selection.table(list -> {
            list.left();

            float iconMul = 1.25f;
            int cols = (int)Mathf.clamp((graphics.getWidth() - Scl.scl(30)) / Scl.scl(32 + 10) / iconMul, 1, 22 / iconMul);
            int count = 0;

            for(Block b : array){
                Image image = new Image(b.uiIcon).setScaling(Scaling.fit);
                list.add(image).size(8 * 4 * iconMul).pad(3);

                ClickListener listener = new ClickListener();
                image.addListener(listener);
                if(!mobile){
                    image.addListener(new HandCursorListener());
                    image.update(() -> image.color.lerp(listener.isOver() || block == b ? Color.white : Color.lightGray, Mathf.clamp(0.4f * TUVars.delta())));
                }else{
                    image.update(() -> image.color.lerp(block == b ? Color.white : Color.lightGray, Mathf.clamp(0.4f * TUVars.delta())));
                }

                image.clicked(() -> {
                    if(input.keyDown(KeyCode.shiftLeft) && Fonts.getUnicode(b.name) != 0){
                        app.setClipboardText((char)Fonts.getUnicode(b.name) + "");
                        ui.showInfoFade("@copied");
                    }else{
                        block = b;
                    }
                });
                BLElements.boxTooltip(image, b.localizedName + (settings.getBool("console") ? "\n[gray]" + b.name : ""));

                if((++count) % cols == 0){
                    list.row();
                }
            }
        }).growX().left().padBottom(10);
    }

    TextureRegion getDirection(){
        TextureRegionDrawable tex = switch(rotation){
            case 1 -> Icon.up;
            case 2 -> Icon.left;
            case 3 -> Icon.down;
            default -> Icon.right;
        };
        return tex.getRegion();
    }

    void placeBlock(){
        if(input.shift()){
            Utils.copyJS("Vars.world.tile(@).setBlock(Vars.content.block(@), Team.get(@), @);",
                placePos, block.id, placeTeam.id, rotation
            );
            return;
        }

        world.tile(placePos).setBlock(block, placeTeam, rotation);
    }

    void deleteBlock(){
        if(input.shift()){
            Utils.copyJS("Vars.world.tile(@).setAir();", placePos);
            return;
        }

        world.tile(placePos).setAir();
    }

    String teamName(){
        return teamDialog.teamName(placeTeam);
    }

    public Block getBlock(){
        return block;
    }
}
