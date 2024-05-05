/*******************************************************************************
 * Copyright (c) 2010-present Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Stuart McCulloch (Sonatype, Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.sisu.plexus;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.codehaus.plexus.component.annotations.Requirement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class RequirementAnnotationTest
{
    @Requirement
    String defaultReq;

    @Requirement( role = String.class )
    String stringReq;

    @Requirement( hint = "named" )
    String namedReq;

    @Requirement( optional = true )
    String optionalReq;

    @Requirement( hints = { "A", "B", "C" } )
    List<?> namedListReq;

    @Requirement( role = String.class, hint = "named" )
    String namedStringReq;

    @Requirement( role = String.class, hints = { "A", "B", "C" } )
    List<String> namedStringListReq;

    @Test
    public void testRequirementImpl()
        throws NoSuchFieldException
    {
        checkBehaviour( "defaultReq" );
        checkBehaviour( "stringReq" );
        checkBehaviour( "namedReq" );
        checkBehaviour( "optionalReq" );
        checkBehaviour( "namedListReq" );
        checkBehaviour( "namedStringReq" );
        checkBehaviour( "namedStringListReq" );

        assertNotEquals( replicate( getRequirement( "defaultReq" ) ), getRequirement( "stringReq" ) );
        assertNotEquals( replicate( getRequirement( "stringReq" ) ), getRequirement( "namedStringReq" ) );
        assertNotEquals( replicate( getRequirement( "defaultReq" ) ), getRequirement( "namedListReq" ) );
        assertNotEquals( replicate( getRequirement( "defaultReq" ) ), getRequirement( "optionalReq" ) );
    }

    private static void checkBehaviour( final String name )
        throws NoSuchFieldException
    {
        final Requirement orig = getRequirement( name );
        final Requirement clone = replicate( orig );

        assertEquals( orig, clone );
        assertEquals( clone, orig );
        assertEquals( clone, clone );
        assertNotEquals( "", clone );

        assertEquals( orig.hashCode(), clone.hashCode() );

        String origToString = orig.toString().replace( "\"", "" ).replace( ".class", "" );
        String cloneToString = clone.toString().replace( '[', '{' ).replace( ']', '}' );
        cloneToString = cloneToString.replace( "class ", "" ).replace( "interface ", "" );

        assertEquals( new HashSet<String>( Arrays.asList( origToString.split( "[(, )]" ) ) ),
                      new HashSet<String>( Arrays.asList( cloneToString.split( "[(, )]" ) ) ) );

        assertEquals( orig.annotationType(), clone.annotationType() );

        try
        {
            final Field role = RequirementImpl.class.getDeclaredField( "role" );
            final Method getName = role.getType().getMethod( "getName" );
            role.setAccessible( true );

            assertEquals( orig.role().getName(), getName.invoke( role.get( clone ) ) );
        }
        catch ( final Exception e )
        {
            fail( e.toString() );
        }
    }

    private static Requirement getRequirement( final String name )
        throws NoSuchFieldException
    {
        return RequirementAnnotationTest.class.getDeclaredField( name ).getAnnotation( Requirement.class );
    }

    @SuppressWarnings( "deprecation" )
    private static Requirement replicate( final Requirement orig )
    {
        final String h = orig.hint();

        return new RequirementImpl( orig.role(), orig.optional(), h.length() > 0 ? new String[] { h } : orig.hints() );
    }
}
