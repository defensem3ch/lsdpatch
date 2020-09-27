package fontEditor;

import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JPanel;

import structures.LSDJFont;

public class FontMap extends JPanel implements java.awt.event.MouseListener {
    private static final long serialVersionUID = -7745908775698863845L;
    private byte[] romImage = null;
    private int fontOffset = -1;
    private int tileZoom = 1;
    private int displayTileSize = 8;

    public interface TileSelectListener {
        void tileSelected(int tile);
    }

    private TileSelectListener tileSelectedListener = null;

    FontMap() {
        addMouseListener(this);
    }

    void setTileSelectListener(TileSelectListener l) {
        tileSelectedListener = l;
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        int widthScale = getWidth() / LSDJFont.FONT_MAP_WIDTH;
        int heightScale = getHeight() / LSDJFont.FONT_MAP_HEIGHT;
        tileZoom = Math.min(widthScale, heightScale);
        tileZoom = Math.max(tileZoom, 1);
        int offsetX = (getWidth() - LSDJFont.FONT_MAP_WIDTH * tileZoom) / 2;
        int offsetY = (getHeight() - LSDJFont.FONT_MAP_HEIGHT * tileZoom) / 2;
        setPreferredSize(new Dimension(LSDJFont.FONT_MAP_WIDTH * tileZoom, LSDJFont.FONT_MAP_HEIGHT * tileZoom));

        for (int tile = 0; tile < LSDJFont.TILE_COUNT; ++tile) {
            paintTile(g, tile, offsetX, offsetY);
        }
    }

    private void switchColor(Graphics g, int c) {
        switch (c & 3) {
            case 0:
                g.setColor(java.awt.Color.white);
                break;
            case 1:
                g.setColor(java.awt.Color.lightGray);
                break;
            case 2:
                g.setColor(java.awt.Color.pink);  // Not used.
                break;
            case 3:
                g.setColor(java.awt.Color.black);
                break;
        }
    }

    private int getColor(int tile, int x, int y) {
        int tileOffset = fontOffset + tile * 16 + y * 2;
        int xMask = 7 - x;
        int value = (romImage[tileOffset] >> xMask) & 1;
        value |= ((romImage[tileOffset + 1] >> xMask) & 1) << 1;
        return value;
    }

    private void paintTile(Graphics g, int tile, int offsetX, int offsetY) {
        displayTileSize = 8 * tileZoom;
        int x = (tile % 8) * displayTileSize;
        int y = (tile / 8) * displayTileSize;

        for (int row = 0; row < 8; ++row) {
            for (int column = 0; column < 8; ++column) {
                switchColor(g, getColor(tile, column, row));
                g.fillRect(offsetX + x + column * tileZoom, offsetY + y + row * tileZoom, tileZoom, tileZoom);
            }
        }
    }

    void setRomImage(byte[] romImage) {
        this.romImage = romImage;
    }

    public byte[] romImage() {
        return romImage;
    }

    void setFontOffset(int fontOffset) {
        this.fontOffset = fontOffset;
        repaint();
    }

    public void mouseEntered(java.awt.event.MouseEvent e) {
    }

    public void mouseExited(java.awt.event.MouseEvent e) {
    }

    public void mouseReleased(java.awt.event.MouseEvent e) {
    }

    public void mousePressed(java.awt.event.MouseEvent e) {
    }

    public void mouseClicked(java.awt.event.MouseEvent e) {
        int offsetX = (getWidth() - LSDJFont.FONT_MAP_WIDTH * tileZoom) / 2;
        int offsetY = (getHeight() - LSDJFont.FONT_MAP_HEIGHT * tileZoom) / 2;

        int realX = e.getX() - offsetX;
        int realY = e.getY() - offsetY;

        if (realX < 0 || realY < 0 || realX > LSDJFont.FONT_MAP_WIDTH * tileZoom || realY > LSDJFont.FONT_MAP_HEIGHT * tileZoom)
            return;

        int tile = (realY / displayTileSize) * LSDJFont.FONT_NUM_TILES_X +
                realX / displayTileSize;
        if (tile < 0 || tile >= LSDJFont.TILE_COUNT)
            return;

        if (tileSelectedListener != null) {
            tileSelectedListener.tileSelected(tile);
        }
    }
}
