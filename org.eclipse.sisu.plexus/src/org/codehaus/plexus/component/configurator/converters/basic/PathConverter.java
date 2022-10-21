/*******************************************************************************
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Minimal facade required to be binary-compatible with legacy Plexus API
 *******************************************************************************/
package org.codehaus.plexus.component.configurator.converters.basic;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.converters.lookup.ConverterLookup;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;

public class PathConverter
    extends AbstractBasicConverter
{
    public boolean canConvert( final Class<?> type )
    {
        return Path.class.equals( type );
    }

    @Override
    public Object fromString( final String value )
        throws ComponentConfigurationException
    {
        // defer creation of actual Path to fromConfiguration(...)
        return value;
    }

    @Override
    public Object fromConfiguration( final ConverterLookup lookup, final PlexusConfiguration configuration,
                                     final Class<?> type, final Class<?> enclosingType, final ClassLoader loader,
                                     final ExpressionEvaluator evaluator, final ConfigurationListener listener )
        throws ComponentConfigurationException
    {
        final Object result =
            super.fromConfiguration( lookup, configuration, type, enclosingType, loader, evaluator, listener );

        if ( result instanceof String )
        {
            // this always assumes the default filesystem
            Path path = Paths.get( (String)result );
            if ( !path.isAbsolute() )
            {
                // relative paths are are given relative to base directory (not necessarily the working directory)
                // therefore make them absolute via ExpressionEvaluator.alignToBaseDirectory(...)
                Path baseDirectory = evaluator.alignToBaseDirectory( new File("") ).toPath();
                // this uses the filesystem of the base directory
                path = baseDirectory.resolve( (String)result );
            }
            return path;
        }
        else
        {
            return result;
        }
    }
}
