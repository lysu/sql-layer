/**
 * Copyright (C) 2009-2013 FoundationDB, LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.foundationdb.server.store;

import com.foundationdb.Transaction;
import com.foundationdb.ais.model.AkibanInformationSchema;
import com.foundationdb.ais.model.Index;
import com.foundationdb.ais.model.IndexName;
import com.foundationdb.ais.model.JoinColumn;
import com.foundationdb.ais.model.NameGenerator;
import com.foundationdb.ais.model.Sequence;
import com.foundationdb.ais.model.TableName;
import com.foundationdb.tuple.Tuple;
import com.foundationdb.util.layers.DirectorySubspace;
import org.apache.commons.codec.binary.Base64;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class FDBNameGenerator implements NameGenerator
{
    private final Transaction txn;
    private final DirectorySubspace directory;
    private final String path;
    private final NameGenerator wrapped;


    public FDBNameGenerator(Transaction txn, DirectorySubspace directory, String path, NameGenerator wrapped) {
        this.txn = txn;
        this.directory = directory;
        this.path = path;
        this.wrapped = wrapped;
    }

    public static Tuple makePath(String path, String schemaName, String groupName) {
        return Tuple.from(path, "table", schemaName, groupName);
    }

    public static Tuple makePath(String path, Index index) {
        IndexName name = index.getIndexName();
        return Tuple.from(path, "table", name.getSchemaName(), name.getTableName(), name.getName());
    }

    public static Tuple makePath(String path, Sequence sequence) {
        TableName seqName = sequence.getSequenceName();
        return Tuple.from(path, "sequence", seqName.getSchemaName(), seqName.getTableName());
    }


    //
    // Directory based generation
    //

    private String generate(Tuple path) {
        // Using createOrOpen() as uniqueness is confirmed elsewhere
        boolean existed = directory.exists(txn, path);
        DirectorySubspace indexDir = directory.createOrOpen(txn, path);
        byte[] packedPrefix = indexDir.pack();
        String treeName = Base64.encodeBase64String(packedPrefix);
        if(!existed) {
            generatedTreeName(treeName);
        }
        return treeName;
    }

    @Override
    public String generateIndexTreeName(Index index) {
        return generate(makePath(path, index));
    }

    @Override
    public String generateGroupTreeName(String schemaName, String groupName) {
        return generate(makePath(path, schemaName, groupName));
    }

    @Override
    public String generateSequenceTreeName(Sequence sequence) {
        return generate(makePath(path, sequence));
    }

    @Override
    public void generatedTreeName(String treeName) {
        wrapped.generatedTreeName(treeName);
    }


    //
    // Trivially wrapped
    //

    @Override
    public int generateTableID(TableName name) {
        return wrapped.generateTableID(name);
    }

    @Override
    public int generateIndexID(int rootTableID) {
        return wrapped.generateIndexID(rootTableID);
    }

    @Override
    public TableName generateIdentitySequenceName(TableName table) {
        return wrapped.generateIdentitySequenceName(table);
    }

    @Override
    public String generateJoinName(TableName parentTable, TableName childTable, List<JoinColumn> joinIndex) {
        return wrapped.generateJoinName(parentTable, childTable, joinIndex);
    }

    @Override
    public String generateJoinName(TableName parentTable,
                                   TableName childTable,
                                   List<String> pkColNames,
                                   List<String> fkColNames) {
        return wrapped.generateJoinName(parentTable, childTable, pkColNames, fkColNames);
    }

    @Override
    public void mergeAIS(AkibanInformationSchema ais) {
        wrapped.mergeAIS(ais);
    }

    @Override
    public void removeTableID(int tableID) {
        wrapped.removeTableID(tableID);
    }

    @Override
    public void removeTreeName(String treeName) {
        wrapped.removeTreeName(treeName);
    }

    @Override
    public Set<String> getTreeNames() {
        return wrapped.getTreeNames();
    }
}
