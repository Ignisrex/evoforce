package com.silverignis.util;

import com.silverignis.entities.Battlefield;

public class PanelGenerator {

    public static Battlefield.PanelType[][] generatePanels(){
        //build the battlefield: 6 columns x 3 rows, left half blue, right half red.
        Battlefield.PanelType[][] panels = new Battlefield.PanelType[Battlefield.COLS][Battlefield.ROWS];
        for (int col = 0; col < Battlefield.COLS; col++) {
            for (int row = 0; row < Battlefield.ROWS; row++) {
                panels[col][row] = Battlefield.PanelType.NORMAL;
            }
        }
        return panels;
    }

    /**
     * Builds a battlefield that mixes several panel themes.
     * The left half still belongs to the player and the right half to the enemy,
     * but each side is sprinkled with hazard / terrain panels so the grid is
     * no longer a flat blue-vs-red split.
     */
    public static Battlefield.PanelType[][] generateMixedPanels(){
        int cols = Battlefield.COLS;
        int rows = Battlefield.ROWS;
        int mid  = cols / 2;

        // Default: player side is blue, enemy side is red.
        Battlefield.PanelType[][] panels = new Battlefield.PanelType[cols][rows];
        for (int col = 0; col < cols; col++) {
            for (int row = 0; row < rows; row++) {
                panels[col][row] = Battlefield.PanelType.NORMAL;
            }
        }

        // Player (blue) side.
        panels[0][0] = Battlefield.PanelType.ICE;
        panels[1][rows - 1] = Battlefield.PanelType.LIGHTNING;
        panels[2][1] = Battlefield.PanelType.NATURE;
        panels[mid - 1][rows / 2] = Battlefield.PanelType.FIRE;

        // Enemy (red) side.
        panels[mid][0] = Battlefield.PanelType.POISON;
        panels[mid + 1][rows - 1] = Battlefield.PanelType.FIRE;
        panels[cols - 2][1] = Battlefield.PanelType.DARK;
        panels[cols - 1][rows / 2] = Battlefield.PanelType.FIRE;

        return panels;
    }
}
