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
package com.jpexs.decompiler.flash.abc.avm2.model;

import com.jpexs.decompiler.flash.abc.avm2.ConstantPool;
import com.jpexs.decompiler.flash.abc.avm2.instructions.AVM2Instruction;
import com.jpexs.decompiler.graph.Graph;
import com.jpexs.decompiler.graph.GraphTargetItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class WithAVM2Item extends AVM2Item {

    public GraphTargetItem scope;
    public List<GraphTargetItem> items;

    public WithAVM2Item(AVM2Instruction instruction, GraphTargetItem scope, List<GraphTargetItem> items) {
        super(instruction, NOPRECEDENCE);
        this.scope = scope;
        this.items = items;
    }

    public WithAVM2Item(AVM2Instruction instruction, GraphTargetItem scope) {
        super(instruction, NOPRECEDENCE);
        this.scope = scope;
        this.items = new ArrayList<>();
    }

    @Override
    public String toString(boolean highlight, ConstantPool constants, HashMap<Integer, String> localRegNames, List<String> fullyQualifiedNames) {
        String ret;
        ret = hilight("with(", highlight) + scope.toString(highlight, constants, localRegNames, fullyQualifiedNames) + hilight(")", highlight) + "\r\n" + hilight("{", highlight) + "\r\n";
        ret += Graph.INDENTOPEN + "\r\n";
        /*for (GraphTargetItem ti : items) {
         ret += ti.toString(constants, localRegNames, fullyQualifiedNames) + "\r\n";
         }*/
        ret += Graph.INDENTCLOSE + "\r\n";
        ret += hilight("}", highlight);
        return ret;
    }

    @Override
    public boolean needsSemicolon() {
        return false;
    }
}
