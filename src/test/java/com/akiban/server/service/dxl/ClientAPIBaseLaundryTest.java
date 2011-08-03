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

package com.akiban.server.service.dxl;

import com.akiban.server.error.ErrorCode;
import com.akiban.server.error.InvalidOperationException;
import com.akiban.server.error.NoSuchTableException;
import com.akiban.server.error.RowDefNotFoundException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public final class ClientAPIBaseLaundryTest {
    private static final ErrorCode[] IGNORED_CODES = {

            ErrorCode.MULTIGENERATIONAL_TABLE, // shouldn't (yet!) be possible to even get to this

            ErrorCode.UNKNOWN, // generic case, so we should throw a generic error
            ErrorCode.INTERNAL_ERROR, // generic casef, so we should throw a generic error
            ErrorCode.INTERNAL_CORRUPTION, // generic case, so we should throw a generic error
            ErrorCode.UNEXPECTED_EXCEPTION, // generic case, so we should throw a generic error
            ErrorCode.UNSUPPORTED_OPERATION, // generic case, so we should throw a generic error

            ErrorCode.SERVER_SHUTDOWN, // shouldn't happen from D*LFunctions layer
            ErrorCode.STALE_AIS, // shouldn't happen from D*LFunctions layer
            ErrorCode.MALFORMED_REQUEST, // shouldn't happen from D*LFunctions layer
            ErrorCode.TABLEDEF_MISMATCH, // D*LFunctions layer will throw TableDefinitionMismatchException directly
            ErrorCode.ROW_OUTPUT, // D*LFunctions layer will throw RowOutputException directly
            ErrorCode.CONCURRENT_MODIFICATION, // DMLFunctions will throw this directly
            ErrorCode.TABLE_DEFINITION_CHANGED, // DMLFunctions will throw this directly

            ErrorCode.NO_REFERENCED_ROW, // TODO: not sure what this means!
            
            ErrorCode.VALIDATION_FAILURE,           // TODO: AISValidation failure
            ErrorCode.INTERNAL_REFERENCES_BROKEN,   // TODO: AISValidation failure 
            ErrorCode.DUPLICATE_COLUMN,             // TODO: AISValidation failure
            ErrorCode.DUPLICATE_GROUP,              // TODO: AISValidation failure
            ErrorCode.GROUP_MULTIPLE_ROOTS,         // TODO: AISValidation failure
            ErrorCode.FK_TYPE_MISMATCH,             // TODO: AISValidation failure
            ErrorCode.PK_NULL_COLUMN,               // TODO: AISValidation failure
    };

    @Parameterized.Parameters
    public static List<Object[]> data() {
        Set<ErrorCode> ignore = new HashSet<ErrorCode>(Arrays.asList(IGNORED_CODES));

        List<Object[]> params = new ArrayList<Object[]>();
        for (ErrorCode errorCode : ErrorCode.values()) {
            if (!ignore.contains(errorCode)) {
                params.add( new Object[]{errorCode});
            }
        }
        return params;
    }

    private final ErrorCode errorCode;

    public ClientAPIBaseLaundryTest(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
    @Test
    public void isLaundered() {
        //InvalidOperationException ioeBasic = new InvalidOperationException(errorCode, "my message");
        //InvalidOperationException ioeLaundered = ClientAPIBase.launder(ioeBasic);

        //if(ioeLaundered.getClass().equals(InvalidOperationException.class)) {
        //    fail(errorCode.name());
        //}
    }
}
