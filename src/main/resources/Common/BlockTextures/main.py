from PIL import Image

# edit filenames if needed
files = {
    "north0": "Mob_Fan_North0.png",
    "north1": "Mob_Fan_North1.png",
    "north2": "Mob_Fan_North2.png",
    "north3": "Mob_Fan_North3.png",
    "south": "Mob_Fan_South.png",
    "east":  "Mob_Fan_East.png",
    "west":  "Mob_Fan_West.png",
    "top":   "Mob_Fan_Up.png",
    "bottom":"Mob_Fan_Down.png",
}

atlas = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
def paste(name, x, y):
    img = Image.open(files[name]).convert("RGBA")
    atlas.paste(img, (x, y))

# 4 frames (reuse north for now)
paste("north0", 0, 0)
paste("north1", 128, 0)
paste("north2", 256, 0)
paste("north3", 384, 0)

# static faces
paste("south", 0, 128)
paste("east", 128, 128)
paste("west", 256, 128)
paste("top", 384, 128)
paste("bottom", 0, 256)

atlas.save("Mob_Fan_Atlas.png")
print("Wrote Mob_Fan_Atlas.png")