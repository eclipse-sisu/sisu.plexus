/*******************************************************************************
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Minimal facade required to be binary-compatible with legacy Plexus API
 *******************************************************************************/
package org.codehaus.plexus.component.configurator.converters.basic;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.converters.composite.MapConverter;
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
        // simple configuration (just a string value)
        if ( configuration.getValue() != null && !configuration.getValue().isEmpty() )
        {
            Object result = super.fromConfiguration( lookup, configuration, type, enclosingType, loader, evaluator, listener );
            if ( result instanceof String )
            {
                // this always assumes the default filesystem
                Path path = getPath( FileSystems.getDefault(), (String) result );
                if ( !path.isAbsolute() )
                {
                    // relative paths are are given relative to base directory (not necessarily the working directory)
                    // therefore make them absolute via ExpressionEvaluator.alignToBaseDirectory(...)
                    return evaluator.alignToBaseDirectory( path.toFile() ).toPath();
                }
                return path;
            }
            return result;
        }
        // complex configuration (containing filesystem URI, parameter, and path)
        else
        {
            try
            {
                Object object = fromExpression( configuration.getChild( "filesystem-uri" ), evaluator, type );
                final URI uri;
                if ( object instanceof String )
                {
                    uri = new URI( (String) object );
                }
                else if ( object instanceof URI )
                {
                    uri = (URI) object;
                }
                else
                {
                    throw new ComponentConfigurationException( "Configuration element with name 'filesystem-uri' does not contain a URI" );
                }
                MapConverter mapConverter = new MapConverter();
                object = mapConverter.fromConfiguration( lookup, configuration.getChild( "filesystem-properties" ), Map.class, enclosingType, loader, evaluator );
                final Map<String,?> env;
                if ( object instanceof Map )
                {
                    env = (Map<String, ?>) object;
                }
                else
                {
                    env = Collections.emptyMap();
                }
                object = fromExpression( configuration.getChild( "path" ), evaluator, type );
                final String path;
                if ( object instanceof String )
                {
                    path = (String) object;
                }
                else
                {
                    throw new ComponentConfigurationException( "Configuration element with name 'path' does not contain a String" );
                }
                FileSystem fileSystem;
                try
                {
                    fileSystem = FileSystems.getFileSystem( uri );
                }
                catch ( FileSystemNotFoundException e )
                {
                    fileSystem = FileSystems.newFileSystem( uri, env );
                    // TODO: where to close the FS?
                    // via DefaultPlexusContainer.dispose()
                    // via PlexusLifecycleManager.unmanage of the surrounding bean
                    // problem: converter is independent from container or lifecycle manager
                }
                // normalize separator?
                return getPath( fileSystem, path );
            }
            catch (IOException | URISyntaxException e)
            {
                throw new ComponentConfigurationException( "Cannot create java.nio.file.Path", e );
            }
        }
    }

    private static Path getPath( FileSystem fileSystem, String path )
    {
        // accept both *nix and Windows separators
        String[] parts = path.split( "/\\\\" );
        return fileSystem.getPath( parts[0], Arrays.copyOfRange( parts, 1, parts.length ) );
    }
}
