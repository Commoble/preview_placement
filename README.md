Preview Placement is a minecraft/neoforge mod which renders previews of blocks about to be placed for specific items specified in resource packs.

This mod is entirely client side, installing it on a server will do nothing (but it won't crash the server either, so it's safe to embed this mod in content mods that exist on both sides).

The purpose of this is so blocks with complicated orientation-placement behavior
(like More Red's logic gates or Hyperbox's hyperboxes, which can be oriented 24 different ways depending on where the player clicks)
can show the player how the block will be placed.

## Depending on the Mod

Mods that wish to jarjar Preview Placement can do so via jarjar:

```gradle
repositories {
	maven {url = "https://maven.commoble.net"}
}

dependencies {
	implementation "net.commoble.preview_placement:preview_placement:${preview_placement_version}"
	jarJar(group: "net.commoble.preview_placement", name: "preview_placement", version: "[${preview_placement_version}, ${preview_placement_max_version})")
}
```

Available versions can be found on the maven: https://maven.commoble.net/net/commoble/preview_placement/preview_placement/

## Config

A config file is created in the (minecraft instance)/config/preview-placement-client.toml which has the following values by default:

```toml
#Render preview of specified blockitems
showPlacementPreview = true
#Opacity of the render preview
# Default: 0.5
# Range: 0.0 ~ 1.0
previewPlacementOpacity = 0.5
```

## Placement Preview Tags (26.1+)

In 26.1+, placement preview blocks/items are defined via the `preview_placement:placement_preview` item tag.

This tag must be defined on the server so it can be sent to the client, though Preview Placement itself does not have to be on the server.

Any blockitems in this item tag will have a placement preview rendered on the client. Currently, only BlockItems are supported.

## Placement Preview Files (1.21.11 only)

Placement Preview files specify which items should render placement previews and what model should be rendered for each blockstate which can be placed.

Compiling against the mod enables datagen helpers in the PlacementPreviewDefinition class. If these sources aren't needed, cursemaven can be used instead for runtime-only dependencies: https://cursemaven.com/

Placement Preview files must have file paths of the form assets/<modid>/preview_placement/placement_preview/<item>.json, where modid:item is the id of a BlockItem item.

They have a json format similar to variant blockstate files, mapping blocks to blockstate selectors to rotated item models. For example:

```json
// assets/minecraft/preview_placement/placement_preview/dirt.json
{
	"minecraft:dirt": { // block id
		"": {"model": "preview_placement:dirt_preview"} // model field refers to an *item json*, not a model json
	}
}
```

Multiple block ids can be specified, for items such as torches that can place different kinds of blocks.

Another example:

```json
// assets/morered/preview_placement/placement_preview/and_gate.json
{
	"morered:and_gate": {
		"facing=down,rotation=0": {
			"model": "morered:and_gate_preview"
		},
		"facing=down,rotation=1": {
			"model": "morered:and_gate_preview",
			"y": 90
		},
		"facing=down,rotation=2": {
			"model": "morered:and_gate_preview",
			"y": 180
		},
		"facing=down,rotation=3": {
			"model": "morered:and_gate_preview",
			"y": 270
		},
		"facing=east,rotation=0": {
			"model": "morered:and_gate_preview",
			"x": 270,
			"y": 90
		},
		"facing=east,rotation=1": {
			"model": "morered:and_gate_preview",
			"y": 180,
			"z": 270
		},
		"facing=east,rotation=2": {
			"model": "morered:and_gate_preview",
			"x": 90,
			"y": 270
		},
		"facing=east,rotation=3": {
			"model": "morered:and_gate_preview",
			"z": 270
		}
	}
}
```

Model fields refer to item jsons, not model jsons.

Blockstate selectors do not need to be exhaustive, states not covered by any selector will have no previews rendered.

Placement Preview jsons can only be created for items which extend BlockItem.

Only variants format is supported, not multipart, and random models are not currently supported.

Referenced item jsons do not have to refer to any registered item:

```json
// assets/preview_placement/item/dirt_preview.json
{
  "model": {
    "type": "model",
    "model": "preview_placement:block/dirt_preview"
  }
}
```

## Placement Preview Models

Preview Placement provides a `preview_placement:placement_preview` model loader which can take an existing model and render it translucently:

```json
// assets/preview_placement/models/item/dirt_preview.json
{
	"loader": "preview_placement:placement_preview",
	"model": {
		"parent": "block/dirt"
	}
}
```
