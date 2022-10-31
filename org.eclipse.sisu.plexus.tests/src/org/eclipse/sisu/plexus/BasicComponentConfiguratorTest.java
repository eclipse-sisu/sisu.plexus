/*******************************************************************************
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.sisu.plexus;

import java.io.File;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

import org.codehaus.plexus.component.configurator.BasicComponentConfigurator;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;
import org.codehaus.plexus.component.configurator.expression.DefaultExpressionEvaluator;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.DefaultPlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;

public class BasicComponentConfiguratorTest
{
    @Rule
    public TemporaryFolder tmpDirectory = new TemporaryFolder();

    private ComponentConfigurator configurator;

    @Before
    public void setUp()
    {
        configurator = new BasicComponentConfigurator();
    }

    @Test
    public void testBasicPathConverterWithSimplePathOnDefaultFileSystem()
        throws ComponentConfigurationException
    {
        PathTestComponent component = new PathTestComponent();
        Path absolutePath = Paths.get( "" ).resolve( "absolute" ).toAbsolutePath();
        configure( component, "path", "readme.txt", "absolutePath", absolutePath.toString(), "file", "readme.txt",
                   "absoluteFile", absolutePath.toString() );
        // path must be converted to absolute one
        assertEquals( tmpDirectory.getRoot().toPath().resolve( "readme.txt" ), component.path );
        assertEquals( FileSystems.getDefault(), component.path.getFileSystem() );
        assertEquals( absolutePath, component.absolutePath );
        assertEquals( new File( tmpDirectory.getRoot(), "readme.txt" ), component.file );
        assertEquals( absolutePath.toFile(), component.absoluteFile );
    }

    @Test
    public void testBasicPathConverterWithComplexPathOnZipFileSystem()
        throws ComponentConfigurationException
    {
        PathTestComponent component = new PathTestComponent();
        final DefaultPlexusConfiguration config = new DefaultPlexusConfiguration( "testConfig" );
        // test with ZIP File System
        // use non-existing file
        File zipFile = new File( tmpDirectory.getRoot(), "example-zip-file4.zip" );
        URI fileSystemUri = URI.create("jar:file:" + zipFile.toString());
        // create file through provider
        Map<String, String> properties = Collections.singletonMap( "create", "true" );
        addComplexConfigurationForPath( config, "path", fileSystemUri, properties, "my/file" );
        configure( component, config );
        // path must be converted to absolute one
        Path actualPath = component.path;
        assertEquals( "jar", actualPath.getFileSystem().provider().getScheme() );
        assertEquals( "my/file", actualPath.toString() );
    }

    private void addComplexConfigurationForPath( DefaultPlexusConfiguration configuration, String name, URI uri, Map<String, String> properties, String path )
    {
        PlexusConfiguration pathConfiguration = new DefaultPlexusConfiguration( name );
        pathConfiguration.addChild( "filesystem-uri", uri.toString() );
        if ( properties != null )
        {
            PlexusConfiguration propertiesConfig =  pathConfiguration.getChild( "filesystem-properties" );
            for ( Map.Entry<String, String> property : properties.entrySet() )
            {
                propertiesConfig.addChild( property.getKey(), property.getValue() );
            }
        }
        pathConfiguration.addChild( "path", path );
        configuration.addChild( pathConfiguration );
    }

    private void configure( Object component, String... keysAndValues )
        throws ComponentConfigurationException
    {
        final DefaultPlexusConfiguration config = new DefaultPlexusConfiguration( "testConfig" );
        if ( keysAndValues.length % 2 != 0 )
        {
            throw new IllegalArgumentException( "Even number of keys and values expected" );
        }
        for ( int i = 0; i < keysAndValues.length; i += 2 )
        {
            config.addChild( keysAndValues[i], keysAndValues[i + 1] );
        }
        configure( component, config );
    }

    private void configure( Object component, PlexusConfiguration config )
        throws ComponentConfigurationException
    {
        final ExpressionEvaluator evaluator = new DefaultExpressionEvaluator()
        {
            @Override
            public File alignToBaseDirectory( File path )
            {
                if ( !path.isAbsolute() )
                {
                    return new File( tmpDirectory.getRoot(), path.getPath() );
                }
                else
                {
                    return path;
                }
            }
        };
        configurator.configureComponent( component, config, evaluator, null );
    }

    static final class PathTestComponent
    {
        Path path;

        Path absolutePath;

        File file;

        File absoluteFile;
    }
}
