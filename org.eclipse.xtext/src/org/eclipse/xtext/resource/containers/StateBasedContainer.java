/*******************************************************************************
 * Copyright (c) 2010, 2017 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.resource.containers;

import static java.util.Collections.*;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.resource.IResourceDescription;
import org.eclipse.xtext.resource.IResourceDescriptions;
import org.eclipse.xtext.resource.impl.ResourceDescriptionsBasedContainer;
import org.eclipse.xtext.xbase.lib.IterableExtensions;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

/**
 * A specialized container which is based on some long living lightweight state.
 * A {@link IContainerState container state} is used to decide about the actually
 * contained resource descriptions.
 * 
 * @author Sebastian Zarnekow - Initial contribution and API
 */
public class StateBasedContainer extends ResourceDescriptionsBasedContainer {

	private final IContainerState state;

	public StateBasedContainer(IResourceDescriptions descriptions, IContainerState state) {
		super(descriptions);
		this.state = state;
	}
	
	@Override
	protected Iterable<IEObjectDescription> filterByURI(Iterable<IEObjectDescription> unfiltered) {
		Predicate<IEObjectDescription> predicate = getStateContainsPredicate(input -> input.getEObjectURI().trimFragment());
		return Iterables.filter(unfiltered, predicate);
	}
	
	protected <T> Predicate<T> getStateContainsPredicate(Function<T, URI> uriProvider) {
		return new Predicate<T>() {
			private Collection<URI> contents = null;

			@Override
			public boolean apply(T input) {
				if(contents == null) {
					contents = state.getContents();
				}
				URI resourceURI = uriProvider.apply(input);
				final boolean contains = contents.contains(resourceURI);
				return contains;
			}
		};
	}

	@Override
	public boolean hasResourceDescription(URI uri) {
		return state.contains(uri);
	}
	
	@Override
	public int getResourceDescriptionCount() {
		return state.getContents().size();
	}

	@Override
	public boolean isEmpty() {
		return state.isEmpty();
	}
	
	@Override
	public IResourceDescription getResourceDescription(URI uri) {
		if (state.contains(uri))
			return getDescriptions().getResourceDescription(uri);
		return null;
	}

	@Override
	public Iterable<IResourceDescription> getResourceDescriptions() {
		if (isEmpty())
			return Collections.emptyList();
		return getUriToDescription().values();
	}

	@Override
	protected Map<URI, IResourceDescription> doGetUriToDescription() {
		Map<URI, IResourceDescription> result = Maps.newLinkedHashMap();
		for(URI uri: state.getContents()) {
			IResourceDescription description = getDescriptions().getResourceDescription(uri);
			if (description != null)
				result.put(uri, description);
		}
		return result;
	}
	
	@Override
	public Iterable<IEObjectDescription> getExportedObjects() {
		if (isEmpty())
			return emptyList();
		return super.getExportedObjects();
	}
	
	@Override
	public Iterable<IEObjectDescription> getExportedObjectsByType(EClass type) {
		if (isEmpty())
			return emptyList();

		Predicate<IResourceDescription> isResourceDescritptionInState = getStateContainsPredicate(input -> input.getURI());
		Iterable<IResourceDescription> resourceDescriptionsInState = Iterables.filter(getDescriptions().getAllResourceDescriptions(), isResourceDescritptionInState);
		return IterableExtensions.flatMap(resourceDescriptionsInState, desc -> desc.getExportedObjectsByType(type));
	}
	
	@Override
	public Iterable<IEObjectDescription> getExportedObjectsByObject(EObject object) {
		if (isEmpty())
			return emptyList();
		return super.getExportedObjectsByObject(object);
	}
	
	@Override
	public Iterable<IEObjectDescription> getExportedObjects(EClass type, QualifiedName qualifiedName, boolean ignoreCase) {
		if (isEmpty())
			return emptyList();
		return super.getExportedObjects(type, qualifiedName, ignoreCase);
	}
	
	/**
	 * @since 2.4
	 */
	@Override
	public String toString() {
		return "["+getClass().getSimpleName()+"] "+ state;
	}

}
