/**
 * Copyright (C) 2011 Akiban Technologies Inc.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */

package com.akiban.sql.optimizer;

import com.akiban.sql.TestBase;
import com.akiban.sql.parser.DMLStatementNode;
import com.akiban.sql.parser.StatementNode;
import com.akiban.sql.parser.SQLParser;

import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runner.RunWith;

import com.akiban.ais.ddl.SchemaDef;
import com.akiban.ais.ddl.SchemaDefToAis;
import com.akiban.ais.model.AkibanInformationSchema;
import com.akiban.ais.model.GroupIndex;
import com.akiban.ais.model.Index;
import com.akiban.ais.model.UserTable;
import com.akiban.server.RowDef;
import com.akiban.server.TableStatus;
import com.akiban.server.util.GroupIndexCreator;

import com.akiban.qp.row.Row;
import com.akiban.qp.rowtype.IndexRowType;
import com.akiban.server.test.it.qp.TestRow;

import org.junit.Before;

import java.util.ArrayList;
import java.util.Collection;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

@RunWith(Parameterized.class)
public class OperatorCompilerTest extends TestBase
{
    public static final File RESOURCE_DIR = 
        new File(OptimizerTestBase.RESOURCE_DIR, "operator");
    
    protected File schemaFile, indexFile;
    protected SQLParser parser;
    protected OperatorCompiler compiler;

    @Before
    public void makeCompiler() throws Exception {
        parser = new SQLParser();
        AkibanInformationSchema ais = loadSchema(schemaFile);
        if (indexFile != null)
            loadGroupIndexes(ais, indexFile);
        compiler = TestOperatorCompiler.create(parser, ais, "user");
    }

    protected static AkibanInformationSchema loadSchema(File schema) throws Exception {
        String sql = fileContents(schema);
        SchemaDef schemaDef = SchemaDef.parseSchema("use user; " + sql);
        SchemaDefToAis toAis = new SchemaDefToAis(schemaDef, false);
        return toAis.getAis();
    }

    protected void loadGroupIndexes(AkibanInformationSchema ais, File file) 
            throws Exception {
        Reader rdr = null;
        try {
            rdr = new FileReader(file);
            BufferedReader brdr = new BufferedReader(rdr);
            while (true) {
                String line = brdr.readLine();
                if (line == null) break;
                String defn[] = line.split("\t");
                GroupIndex index = GroupIndexCreator.createIndex(ais,
                                                                 defn[0], 
                                                                 defn[1],
                                                                 defn[2]);
                index.getGroup().addIndex(index);
            }
        }
        finally {
            if (rdr != null) {
                try {
                    rdr.close();
                }
                catch (IOException ex) {
                }
            }
        }
    }

    public static class TestOperatorCompiler extends OperatorCompiler {
        public static OperatorCompiler create(SQLParser parser, 
                                              AkibanInformationSchema ais, 
                                              String defaultSchemaName) {
            // This just needs to be enough to keep from UserTableRowType
            // constructor from getting NPE.
            int tableId = 0;
            for (UserTable userTable : ais.getUserTables().values()) {
                new RowDef(userTable, new TableStatus(++tableId));
            }
            return new TestOperatorCompiler(parser, ais, "user");
        }

        private TestOperatorCompiler(SQLParser parser, 
                                     AkibanInformationSchema ais, 
                                     String defaultSchemaName) {
            super(parser, ais, defaultSchemaName);
        }
    }

    @Parameters
    public static Collection<Object[]> statements() throws Exception {
        Collection<Object[]> result = new ArrayList<Object[]>();
        for (File subdir : RESOURCE_DIR.listFiles(new FileFilter() {
                public boolean accept(File file) {
                    return file.isDirectory();
                }
            })) {
            File schemaFile = new File(subdir, "schema.ddl");
            if (schemaFile.exists()) {
                File indexFile = new File(subdir, "group.idx");
                if (!indexFile.exists())
                    indexFile = null;
                for (Object[] args : sqlAndExpected(subdir)) {
                    Object[] nargs = new Object[args.length+2];
                    nargs[0] = subdir.getName() + "/" + args[0];
                    nargs[1] = schemaFile;
                    nargs[2] = indexFile;
                    System.arraycopy(args, 1, nargs, 3, args.length-1);
                    result.add(nargs);
                }
            }
        }
        return result;
    }

    public OperatorCompilerTest(String caseName, File schemaFile, File indexFile,
                                String sql, String expected) {
        super(caseName, sql, expected);
        this.schemaFile = schemaFile;
        this.indexFile = indexFile;
    }

    @Test
    public void testOperator() throws Exception {
        StatementNode stmt = parser.parseStatement(sql);
        OperatorCompiler.Result result = compiler.compile((DMLStatementNode)stmt);
        assertEqualsWithoutHashes(caseName, expected, result.toString());
    }

}