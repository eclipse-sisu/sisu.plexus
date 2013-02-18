/*******************************************************************************
 * Copyright (c) 2010, 2013 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stuart McCulloch (Sonatype, Inc.) - initial API and implementation
 *******************************************************************************/
package org.codehaus.plexus.component.configurator.converters.composite;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.converters.AbstractConfigurationConverter;
import org.codehaus.plexus.component.configurator.converters.ParameterizedConfigurationConverter;
import org.codehaus.plexus.component.configurator.converters.lookup.ConverterLookup;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;

public class CollectionConverter
    extends AbstractConfigurationConverter
    implements ParameterizedConfigurationConverter
{
    public boolean canConvert( final Class<?> type )
    {
        return Collection.class.isAssignableFrom( type ) && !Map.class.isAssignableFrom( type );
    }

    public Object fromConfiguration( final ConverterLookup lookup, final PlexusConfiguration configuration,
                                     final Class<?> type, final Class<?> enclosingType, final ClassLoader loader,
                                     final ExpressionEvaluator evaluator, final ConfigurationListener listener )
        throws ComponentConfigurationException
    {
        return fromConfiguration( lookup, configuration, type, null, enclosingType, loader, evaluator, listener );
    }

    public Object fromConfiguration( final ConverterLookup lookup, final PlexusConfiguration configuration,
                                     final Class<?> type, final Type[] parameterTypes, final Class<?> enclosingType,
                                     final ClassLoader loader, final ExpressionEvaluator evaluator,
                                     final ConfigurationListener listener )
        throws ComponentConfigurationException
    {
        final Object value = fromExpression( configuration, evaluator );
        if ( type.isInstance( value ) )
        {
            return value;
        }
        try
        {
            final Collection<Object> collection = instantiateCollection( configuration, type, loader );
            if ( value instanceof Object[] )
            {
                Collections.addAll( collection, (Object[]) value );
            }
            else
            {
                final CollectionHelper helper = new CollectionHelper( lookup, loader, evaluator, listener );
                if ( null == value )
                {
                    helper.addAll( collection, parameterTypes, enclosingType, configuration );
                }
                else if ( value instanceof String && ( "".equals( value ) || !value.equals( configuration.getValue() ) ) )
                {
                    helper.addAll( collection, parameterTypes, enclosingType, csvToXml( configuration, (String) value ) );
                }
                else
                {
                    failIfNotTypeCompatible( value, type, configuration );
                }
            }
            return collection;
        }
        catch ( final ComponentConfigurationException e )
        {
            if ( null == e.getFailedConfiguration() )
            {
                e.setFailedConfiguration( configuration );
            }
            throw e;
        }
        catch ( final IllegalArgumentException e )
        {
            throw new ComponentConfigurationException( configuration, "Cannot store value into collection", e );
        }
    }

    @SuppressWarnings( "unchecked" )
    private Collection<Object> instantiateCollection( final PlexusConfiguration configuration, final Class<?> type,
                                                      final ClassLoader loader )
        throws ComponentConfigurationException
    {
        final Class<?> implType = getClassForImplementationHint( type, configuration, loader );
        if ( null == implType || Modifier.isAbstract( implType.getModifiers() ) )
        {
            if ( Set.class.isAssignableFrom( type ) )
            {
                if ( SortedSet.class.isAssignableFrom( type ) )
                {
                    return new TreeSet<Object>();
                }
                return new HashSet<Object>();
            }
            return new ArrayList<Object>();
        }

        final Object impl = instantiateObject( implType );
        failIfNotTypeCompatible( impl, type, configuration );
        return (Collection<Object>) impl;
    }

    private static PlexusConfiguration csvToXml( final PlexusConfiguration configuration, final String csv )
    {
        final PlexusConfiguration xml = new XmlPlexusConfiguration( configuration.getName() );
        for ( final String token : csv.split( ",", -1 ) )
        {
            xml.addChild( "#", token );
        }
        return xml;
    }
}
