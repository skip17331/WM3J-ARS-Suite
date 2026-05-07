// Morse Trainer Keyer Enclosure
// =============================
// Parametric box sized for Arduino Pro Micro OR Raspberry Pi Zero.
// Set BOARD to "PROMICRO" or "PIZERO" to switch.
//
// Render to STL:
//   openscad -o keyer-box.stl keyer-box.scad
// Or open in OpenSCAD GUI to tweak.

/* [Board] */
BOARD       = "PROMICRO"; // [PROMICRO, PIZERO]

/* [Wall thickness] */
WALL        = 2.0;
LID_GAP     = 0.4;        // tolerance for snap-fit

/* [Internal padding around board] */
PAD_X       = 3;
PAD_Y       = 3;
PAD_TOP     = 8;          // headroom above board for headers/jack solder

/* [3.5 mm jack cutout] */
JACK_DIAM   = 6.5;
JACK_FROM_BOTTOM = 8;     // height above floor

/* [Piezo cutout (optional)] */
INCLUDE_PIEZO = true;
PIEZO_DIAM    = 12;

/* [Snap clips] */
CLIP_W      = 8;
CLIP_H      = 1.5;

/* ========================================================== */
// Board dimensions (mm)
PROMICRO_X = 33.0;  PROMICRO_Y = 18.0;  PROMICRO_Z = 6.0;   USB_W_PROMICRO = 8.5;  USB_H_PROMICRO = 3.5;
PIZERO_X   = 65.0;  PIZERO_Y   = 30.0;  PIZERO_Z   = 6.0;   USB_W_PIZERO   = 8.5;  USB_H_PIZERO   = 3.5;

board_x = (BOARD == "PIZERO") ? PIZERO_X : PROMICRO_X;
board_y = (BOARD == "PIZERO") ? PIZERO_Y : PROMICRO_Y;
board_z = (BOARD == "PIZERO") ? PIZERO_Z : PROMICRO_Z;
usb_w   = (BOARD == "PIZERO") ? USB_W_PIZERO : USB_W_PROMICRO;
usb_h   = (BOARD == "PIZERO") ? USB_H_PIZERO : USB_H_PROMICRO;

inner_x = board_x + 2 * PAD_X;
inner_y = board_y + 2 * PAD_Y;
inner_z = board_z + PAD_TOP;

outer_x = inner_x + 2 * WALL;
outer_y = inner_y + 2 * WALL;
outer_z = inner_z + WALL;        // floor only; lid is separate

/* ============================ Box body =================================== */
module box_body() {
    difference() {
        // outer shell
        cube([outer_x, outer_y, outer_z]);

        // hollow interior
        translate([WALL, WALL, WALL])
            cube([inner_x, inner_y, inner_z + 1]);

        // USB cutout on -X face, centered on board
        translate([-1, WALL + PAD_Y + (board_y - usb_w)/2, WALL + 1.0])
            cube([WALL + 2, usb_w, usb_h]);

        // 3.5 mm jack cutout on +X face
        translate([outer_x - WALL - 1, outer_y/2, JACK_FROM_BOTTOM])
            rotate([0, 90, 0])
                cylinder(h = WALL + 2, d = JACK_DIAM, $fn = 32);

        // Optional piezo cutout on +Y face
        if (INCLUDE_PIEZO) {
            translate([outer_x/2, outer_y - WALL - 1, JACK_FROM_BOTTOM + 6])
                rotate([-90, 0, 0])
                    cylinder(h = WALL + 2, d = PIEZO_DIAM, $fn = 32);
        }

        // Lid clip slots on +X and -X walls
        for (x = [-0.1, outer_x - WALL + 0.1]) {
            translate([x, (outer_y - CLIP_W)/2, outer_z - CLIP_H - 0.5])
                cube([WALL + 0.2, CLIP_W, CLIP_H + 0.6]);
        }
    }

    // Standoffs (4 corners) for board mounting
    standoff_h = WALL + 1.5;
    for (px = [WALL + PAD_X + 1, WALL + PAD_X + board_x - 1])
        for (py = [WALL + PAD_Y + 1, WALL + PAD_Y + board_y - 1])
            translate([px, py, WALL])
                cylinder(h = standoff_h, d = 3, $fn = 16);
}

/* ============================ Lid ======================================== */
module lid() {
    lid_x = outer_x - 2 * LID_GAP;
    lid_y = outer_y - 2 * LID_GAP;
    union() {
        // top plate
        translate([LID_GAP, LID_GAP, 0])
            cube([lid_x, lid_y, WALL]);

        // snap clips
        for (x_offset = [LID_GAP + WALL/2, LID_GAP + lid_x - WALL/2 - WALL])
            translate([x_offset, (outer_y - CLIP_W)/2, -CLIP_H + 0.2])
                cube([WALL, CLIP_W, CLIP_H]);
    }
}

/* ============================ Layout ===================================== */
// Print box and lid side by side.
box_body();
translate([outer_x + 10, 0, 0]) lid();
