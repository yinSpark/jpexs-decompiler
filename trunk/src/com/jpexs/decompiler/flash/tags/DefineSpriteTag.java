/*
 *  Copyright (C) 2010-2013 JPEXS
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.jpexs.decompiler.flash.tags;

import com.jpexs.decompiler.flash.Configuration;
import com.jpexs.decompiler.flash.SWF;
import com.jpexs.decompiler.flash.SWFInputStream;
import com.jpexs.decompiler.flash.SWFOutputStream;
import com.jpexs.decompiler.flash.abc.CopyOutputStream;
import com.jpexs.decompiler.flash.tags.base.BoundedTag;
import com.jpexs.decompiler.flash.tags.base.CharacterTag;
import com.jpexs.decompiler.flash.tags.base.Container;
import com.jpexs.decompiler.flash.tags.base.ContainerItem;
import com.jpexs.decompiler.flash.tags.base.DrawableTag;
import com.jpexs.decompiler.flash.tags.base.PlaceObjectTypeTag;
import com.jpexs.decompiler.flash.types.MATRIX;
import com.jpexs.decompiler.flash.types.RECT;
import com.jpexs.helpers.Cache;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

/**
 * Defines a sprite character
 */
public class DefineSpriteTag extends CharacterTag implements Container, BoundedTag, DrawableTag {

    /**
     * Character ID of sprite
     */
    public int spriteId;
    /**
     * Number of frames in sprite
     */
    public int frameCount;
    /**
     * A series of tags
     */
    public List<Tag> subTags;
    private int level;
    public static final int ID = 39;

    @Override
    public int getCharacterId() {
        return spriteId;
    }

    private RECT getCharacterBounds(HashMap<Integer, CharacterTag> allCharacters, Set<Integer> characters, Stack<Integer> visited) {
        if (visited.contains(spriteId)) {
            return new RECT();
        }
        visited.push(spriteId);
        RECT ret = new RECT(Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE);
        boolean foundSomething = false;
        for (int c : characters) {
            Tag t = allCharacters.get(c);
            RECT r = null;
            if (t instanceof BoundedTag) {
                if (t instanceof CharacterTag) {
                    if (visited.contains(((CharacterTag) t).getCharacterId())) {
                        continue;
                    }
                }
                r = ((BoundedTag) t).getRect(allCharacters, visited);
            }
            if (r != null) {
                foundSomething = true;
                ret.Xmin = Math.min(r.Xmin, ret.Xmin);
                ret.Ymin = Math.min(r.Ymin, ret.Ymin);
                ret.Xmax = Math.max(r.Xmax, ret.Xmax);
                ret.Ymax = Math.max(r.Ymax, ret.Ymax);
            }
        }
        visited.pop();
        if (!foundSomething) {
            return new RECT();
        }
        return ret;
    }
    private static Cache rectCache = Cache.getInstance(true);

    @Override
    public RECT getRect(HashMap<Integer, CharacterTag> characters, Stack<Integer> visited) {
        if (rectCache.contains(this)) {
            return (RECT) rectCache.get(this);
        }
        if (visited.contains(spriteId)) {
            return new RECT();
        }
        visited.push(spriteId);
        RECT emptyRet = new RECT();
        RECT ret = new RECT(Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE);
        HashMap<Integer, Integer> depthMap = new HashMap<>();
        boolean foundSomething = false;
        int pos = 0;
        for (Tag t : subTags) {
            pos++;
            MATRIX m = null;
            int characterId = -1;
            if (t instanceof PlaceObjectTypeTag) {
                PlaceObjectTypeTag pot = (PlaceObjectTypeTag) t;
                m = pot.getMatrix();
                int charId = pot.getCharacterId();
                if (charId > -1) {
                    depthMap.put(pot.getDepth(), charId);
                    characterId = (charId);
                } else {
                    Integer chi = (depthMap.get(pot.getDepth()));
                    if (chi != null) {
                        characterId = chi;
                    }
                }
            }
            if (characterId == -1) {
                continue;
            }
            HashSet<Integer> need = new HashSet<>();
            if (visited.contains(characterId)) {
                continue;
            }
            need.add(characterId);
            visited.pop();
            RECT r = getCharacterBounds(characters, need, visited);
            visited.push(spriteId);

            if (m != null) {
                AffineTransform trans = SWF.matrixToTransform(m);

                Point topleft = new Point();
                trans.transform(new Point(r.Xmin, r.Ymin), topleft);
                Point topright = new Point();
                trans.transform(new Point(r.Xmax, r.Ymin), topright);
                Point bottomright = new Point();
                trans.transform(new Point(r.Xmax, r.Ymax), bottomright);
                Point bottomleft = new Point();
                trans.transform(new Point(r.Xmin, r.Ymax), bottomleft);

                r.Xmin = Math.min(Math.min(Math.min(topleft.x, topright.x), bottomleft.x), bottomright.x);
                r.Ymin = Math.min(Math.min(Math.min(topleft.y, topright.y), bottomleft.y), bottomright.y);
                r.Xmax = Math.max(Math.max(Math.max(topleft.x, topright.x), bottomleft.x), bottomright.x);
                r.Ymax = Math.max(Math.max(Math.max(topleft.y, topright.y), bottomleft.y), bottomright.y);

            }
            ret.Xmin = Math.min(r.Xmin, ret.Xmin);
            ret.Ymin = Math.min(r.Ymin, ret.Ymin);
            ret.Xmax = Math.max(r.Xmax, ret.Xmax);
            ret.Ymax = Math.max(r.Ymax, ret.Ymax);
            foundSomething = true;
        }
        visited.pop();

        if (!foundSomething) {
            ret = new RECT();
        }
        rectCache.put(this, ret);
        return ret;
    }

    /**
     * Constructor
     *
     * @param swf
     * @param data Data bytes
     * @param version SWF version
     * @param level
     * @param pos
     * @param parallel
     * @param skipUnusualTags
     * @throws IOException
     */
    public DefineSpriteTag(SWF swf, byte data[], int version, int level, long pos, boolean parallel, boolean skipUnusualTags) throws IOException {
        super(swf, ID, "DefineSprite", data, pos);
        SWFInputStream sis = new SWFInputStream(new ByteArrayInputStream(data), version, pos);
        spriteId = sis.readUI16();
        frameCount = sis.readUI16();
        subTags = sis.readTagList(swf, level + 1, parallel, skipUnusualTags);
    }
    static int c = 0;

    /**
     * Gets data bytes
     *
     * @param version SWF version
     * @return Bytes of data
     */
    @Override
    public byte[] getData(int version) {
        if (Configuration.DISABLE_DANGEROUS) {
            return super.getData(version);
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStream os = baos;
        if (Configuration.DEBUG_COPY) {
            os = new CopyOutputStream(os, new ByteArrayInputStream(super.data));
        }
        SWFOutputStream sos = new SWFOutputStream(os, version);
        try {
            sos.writeUI16(spriteId);
            sos.writeUI16(frameCount);
            sos.writeTags(subTags);
            sos.writeUI16(0);
            sos.close();
        } catch (IOException e) {
        }
        return baos.toByteArray();
    }

    /**
     * Returns all sub-items
     *
     * @return List of sub-items
     */
    @Override
    public List<ContainerItem> getSubItems() {
        List<ContainerItem> ret = new ArrayList<>();
        ret.addAll(subTags);
        return ret;
    }

    /**
     * Returns number of sub-items
     *
     * @return Number of sub-items
     */
    @Override
    public int getItemCount() {
        return subTags.size();
    }

    @Override
    public boolean hasSubTags() {
        return true;
    }

    @Override
    public List<Tag> getSubTags() {
        return subTags;
    }

    @Override
    public Set<Integer> getNeededCharacters() {
        Set<Integer> ret = new HashSet<>();
        for (Tag t : subTags) {
            ret.addAll(t.getNeededCharacters());
        }
        return ret;
    }

    @Override
    public BufferedImage toImage(int frame, List<Tag> tags, RECT displayRect, HashMap<Integer, CharacterTag> characters, Stack<Integer> visited) {
        if (visited.contains(spriteId)) {
            return new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR);
        }
        /* 
         rect.Xmax=displayRect.Xmin+rect.getWidth();
         rect.Ymax=displayRect.Ymin+rect.getWidth();
         rect.Xmin=displayRect.Xmin;
         rect.Ymin=displayRect.Ymin;
         RECT rect=getRect(characters);
         SWF.fixRect(rect);*/
        RECT rect = getRect(characters, visited);
        visited.push(spriteId);
        BufferedImage ret = SWF.frameToImage(spriteId, frame, tags, subTags, rect, frameCount, visited);
        visited.pop();
        return ret;
    }

    @Override
    public Point getImagePos(int frame, HashMap<Integer, CharacterTag> characters, Stack<Integer> visited) {
        //RECT displayRect = getRect(characters, visited); //use visited
        return new Point(0, 0); //displayRect.Xmin,displayRect.Ymin);
    }

    @Override
    public int getNumFrames() {
        return frameCount;
    }
}
