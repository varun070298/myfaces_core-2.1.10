/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.myfaces.view.facelets.tag.jsf;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import javax.faces.FacesException;
import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UINamingContainer;
import javax.faces.component.UIPanel;
import javax.faces.component.UIViewRoot;
import javax.faces.component.UniqueIdVendor;
import javax.faces.context.FacesContext;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagAttributeException;

import org.apache.myfaces.shared.config.MyfacesConfig;
import org.apache.myfaces.view.facelets.ComponentState;
import org.apache.myfaces.view.facelets.DefaultFaceletsStateManagementStrategy;
import org.apache.myfaces.view.facelets.FaceletCompositionContext;
import org.apache.myfaces.view.facelets.FaceletViewDeclarationLanguage;

/**
 * 
 * @author Jacob Hookom
 * @version $Id: ComponentSupport.java 1395401 2012-10-07 22:29:10Z lu4242 $
 */
public final class ComponentSupport
{

    private final static String MARK_DELETED = "oam.vf.MARK_DELETED";
    public final static String MARK_CREATED = "oam.vf.MARK_ID";
    
    /**
     * The UIPanel components, which are dynamically generated to serve as a container for
     * facets with multiple non panel children, are marked with this attribute.
     * This constant is duplicate in javax.faces.webapp.UIComponentClassicTagBase
     */
    public final static String FACET_CREATED_UIPANEL_MARKER = "oam.vf.createdUIPanel";

    /**
     * Special myfaces core marker to indicate the component is handled by a facelet tag handler,
     * so its creation is not handled by user programatically and PSS remove listener should
     * not register it when a remove happens.
     */
    public final static String COMPONENT_ADDED_BY_HANDLER_MARKER = "oam.vf.addedByHandler";

    /**
     * The key under the facelet state map is stored
     */
    public final static String FACELET_STATE_INSTANCE = "oam.FACELET_STATE_INSTANCE";

    /**
     * Used in conjunction with markForDeletion where any UIComponent marked will be removed.
     * 
     * @deprecated use FaceletCompositionContext.finalizeForDeletion
     * @param component
     *            UIComponent to finalize
     */
    @Deprecated
    public static void finalizeForDeletion(UIComponent component)
    {
        // remove any existing marks of deletion
        component.getAttributes().remove(MARK_DELETED);

        // finally remove any children marked as deleted
        if (component.getChildCount() > 0)
        {
            for (Iterator<UIComponent> iter = component.getChildren().iterator(); iter.hasNext();)
            {
                UIComponent child = iter.next();
                if (child.getAttributes().containsKey(MARK_DELETED))
                {
                    iter.remove();
                }
            }
        }

        // remove any facets marked as deleted
        Map<String, UIComponent> facets = component.getFacets();
        if (!facets.isEmpty())
        {
            Collection<UIComponent> col = facets.values();
            for (Iterator<UIComponent> itr = col.iterator(); itr.hasNext();)
            {
                UIComponent fc = itr.next();
                if (fc.getAttributes().containsKey(MARK_DELETED))
                {
                    itr.remove();
                }
            }
        }
    }

    /**
     * A lighter-weight version of UIComponent's findChild.
     * 
     * @param parent
     *            parent to start searching from
     * @param id
     *            to match to
     * @return UIComponent found or null
     */
    public static UIComponent findChild(UIComponent parent, String id)
    {
        int childCount = parent.getChildCount();
        if (childCount > 0)
        {
            for (int i = 0; i < childCount; i++)
            {
                UIComponent child = parent.getChildren().get(i);
                if (id.equals(child.getId()))
                {
                    return child;
                }
            }
        }

        return null;
    }

    /**
     * By TagId, find Child
     * 
     * @param parent
     * @param id
     * @return
     */
    public static UIComponent findChildByTagId(UIComponent parent, String id)
    {
        Iterator<UIComponent> itr = null;
        if (parent.getChildCount() > 0)
        {
            for (int i = 0, childCount = parent.getChildCount(); i < childCount; i ++)
            {
                UIComponent child = parent.getChildren().get(i);
                if (id.equals(child.getAttributes().get(MARK_CREATED)))
                {
                    return child;
                }
            }
        }
        if (parent.getFacetCount() > 0)
        {
            itr = parent.getFacets().values().iterator();
            while (itr.hasNext())
            {
                UIComponent facet = itr.next();
                // check if this is a dynamically generated UIPanel
                if (Boolean.TRUE.equals(facet.getAttributes()
                             .get(FACET_CREATED_UIPANEL_MARKER)))
                {
                    // only check the children and facets of the panel
                    if (facet.getChildCount() > 0)
                    {
                        for (int i = 0, childCount = facet.getChildCount(); i < childCount; i ++)
                        {
                            UIComponent child = facet.getChildren().get(i);
                            if (id.equals(child.getAttributes().get(MARK_CREATED)))
                            {
                                return child;
                            }
                        }
                    }
                    if (facet.getFacetCount() > 0)
                    {
                        Iterator<UIComponent> itr2 = facet.getFacets().values().iterator();
                        while (itr2.hasNext())
                        {
                            UIComponent child = itr2.next();
                            if (id.equals(child.getAttributes().get(MARK_CREATED)))
                            {
                                return child;
                            }
                        }
                    }
                }
                else if (id.equals(facet.getAttributes().get(MARK_CREATED)))
                {
                    return facet;
                }
            }
        }

        return null;
    }

    /**
     * According to JSF 1.2 tag specs, this helper method will use the TagAttribute passed in determining the Locale
     * intended.
     * 
     * @param ctx
     *            FaceletContext to evaluate from
     * @param attr
     *            TagAttribute representing a Locale
     * @return Locale found
     * @throws TagAttributeException
     *             if the Locale cannot be determined
     */
    public static Locale getLocale(FaceletContext ctx, TagAttribute attr) throws TagAttributeException
    {
        Object obj = attr.getObject(ctx);
        if (obj instanceof Locale)
        {
            return (Locale) obj;
        }
        if (obj instanceof String)
        {
            String s = (String) obj;
            if (s.length() == 2)
            {
                return new Locale(s);
            }

            if (s.length() == 5)
            {
                return new Locale(s.substring(0, 2), s.substring(3, 5).toUpperCase());
            }

            if (s.length() >= 7)
            {
                return new Locale(s.substring(0, 2), s.substring(3, 5).toUpperCase(), s.substring(6, s.length()));
            }

            throw new TagAttributeException(attr, "Invalid Locale Specified: " + s);
        }
        else
        {
            throw new TagAttributeException(attr, "Attribute did not evaluate to a String or Locale: " + obj);
        }
    }

    /**
     * Tries to walk up the parent to find the UIViewRoot, if not found, then go to FaceletContext's FacesContext for
     * the view root.
     * 
     * @param ctx
     *            FaceletContext
     * @param parent
     *            UIComponent to search from
     * @return UIViewRoot instance for this evaluation
     */
    public static UIViewRoot getViewRoot(FaceletContext ctx, UIComponent parent)
    {
        UIComponent c = parent;
        do
        {
            if (c instanceof UIViewRoot)
            {
                return (UIViewRoot) c;
            }
            else
            {
                c = c.getParent();
            }
        } while (c != null);

        return ctx.getFacesContext().getViewRoot();
    }
    
    /**
     * Marks all direct children and Facets with an attribute for deletion.
     * 
     * @deprecated use FaceletCompositionContext.markForDeletion
     * @see #finalizeForDeletion(UIComponent)
     * @param component
     *            UIComponent to mark
     */
    @Deprecated
    public static void markForDeletion(UIComponent component)
    {
        // flag this component as deleted
        component.getAttributes().put(MARK_DELETED, Boolean.TRUE);

        Iterator<UIComponent> iter = component.getFacetsAndChildren();
        while (iter.hasNext())
        {
            UIComponent child = iter.next();
            if (child.getAttributes().containsKey(MARK_CREATED))
            {
                child.getAttributes().put(MARK_DELETED, Boolean.TRUE);
            }
        }
    }

    public static void encodeRecursive(FacesContext context, UIComponent toRender) throws IOException, FacesException
    {
        if (toRender.isRendered())
        {
            toRender.encodeBegin(context);
            
            if (toRender.getRendersChildren())
            {
                toRender.encodeChildren(context);
            }
            else if (toRender.getChildCount() > 0)
            {
                for (int i = 0, childCount = toRender.getChildCount(); i < childCount; i++)
                {
                    UIComponent child = toRender.getChildren().get(i);
                    encodeRecursive(context, child);
                }
            }
            
            toRender.encodeEnd(context);
        }
    }

    public static void removeTransient(UIComponent component)
    {
        if (component.getChildCount() > 0)
        {
            for (Iterator<UIComponent> itr = component.getChildren().iterator(); itr.hasNext();)
            {
                UIComponent child = itr.next();
                if (child.isTransient())
                {
                    itr.remove();
                }
                else
                {
                    removeTransient(child);
                }
            }
        }
        
        Map<String, UIComponent> facets = component.getFacets();
        if (!facets.isEmpty())
        {
            for (Iterator<UIComponent> itr = facets.values().iterator(); itr.hasNext();)
            {
                UIComponent facet = itr.next();
                if (facet.isTransient())
                {
                    itr.remove();
                }
                else
                {
                    removeTransient(facet);
                }
            }
        }
    }

    /**
     * Determine if the passed component is not null and if it's new to the tree. This operation can be used for
     * determining if attributes should be wired to the component.
     * 
     * @deprecated use ComponentHandler.isNew
     * @param component
     *            the component you wish to modify
     * @return true if it's new
     */
    @Deprecated
    public static boolean isNew(UIComponent component)
    {
        return component != null && component.getParent() == null;
    }
    
    /**
     * Create a new UIPanel for the use as a dynamically 
     * created container for multiple children in a facet.
     * Duplicate in javax.faces.webapp.UIComponentClassicTagBase.
     * @param facesContext
     * @return
     */
    private static UIComponent createFacetUIPanel(FaceletContext ctx, UIComponent parent, String facetName)
    {
        FacesContext facesContext = ctx.getFacesContext();
        UIComponent panel = facesContext.getApplication().createComponent(facesContext, UIPanel.COMPONENT_TYPE, null);
        
        // The panel created by this method is special. To be restored properly and do not
        // create duplicate ids or any other unwanted conflicts, it requires an unique id.
        // This code is usually called when more than one component is added to a facet and
        // it is necessary to create a shared container.
        // Use FaceletCompositionContext.generateUniqueComponentId() is not possible, because
        // <c:if> blocks inside a facet will make component ids unstable. Use UniqueIdVendor
        // is feasible but also will be affected by <c:if> blocks inside a facet.
        // The only solution that will generate real unique ids is use the parent id and the
        // facet name and derive an unique id that cannot be generated by SectionUniqueIdCounter,
        // doing the same trick as with metadata: use a double __ and add a prefix (f).
        // Note this id will never be printed into the response, because this is just a container.
        FaceletCompositionContext mctx = FaceletCompositionContext.getCurrentInstance(ctx);
        UniqueIdVendor uniqueIdVendor = mctx.getUniqueIdVendorFromStack();
        if (uniqueIdVendor == null)
        {
            uniqueIdVendor = ComponentSupport.getViewRoot(ctx, parent);
        }
        if (uniqueIdVendor != null)
        {
            // UIViewRoot implements UniqueIdVendor, so there is no need to cast to UIViewRoot
            // and call createUniqueId(). See ComponentTagHandlerDelegate
            int index = facetName.indexOf('.');
            String cleanFacetName = facetName;
            if (index >= 0)
            {
                cleanFacetName = facetName.replace('.', '_');
            }
            panel.setId(uniqueIdVendor.createUniqueId(facesContext, 
                    mctx.getSharedStringBuilder()
                      .append(parent.getId())
                      .append("__f_")
                      .append(cleanFacetName).toString()));
        }
        panel.getAttributes().put(FACET_CREATED_UIPANEL_MARKER, Boolean.TRUE);
        panel.getAttributes().put(ComponentSupport.COMPONENT_ADDED_BY_HANDLER_MARKER, Boolean.TRUE);
        return panel;
    }
    
    public static void addFacet(FaceletContext ctx, UIComponent parent, UIComponent c, String facetName)
    {
        // facets now can have multiple children and the direct
        // child of a facet is always an UIPanel (since 2.0)
        UIComponent facet = parent.getFacets().get(facetName);
        if (facet == null)
        {
            //Just set it directly like  before
            parent.getFacets().put(facetName, c);
        }
        else if (!(facet instanceof UIPanel))
        {
            // there is a facet, but it is not an instance of UIPanel
            UIComponent child = facet;
            facet = createFacetUIPanel(ctx, parent, facetName);
            facet.getChildren().add(child);
            facet.getChildren().add(c);
            parent.getFacets().put(facetName, facet);
        }
        else
        {
            // we have a facet, which is an instance of UIPanel at this point
            // check if it is a facet marked UIPanel
            if (Boolean.TRUE.equals(facet.getAttributes().get(FACET_CREATED_UIPANEL_MARKER)))
            {
                facet.getChildren().add(c);
            }
            else
            {
                // the facet is an instance of UIPanel, but it is not marked,
                // so we have to create a new UIPanel and store this one in it
                UIComponent oldPanel = facet;
                facet = createFacetUIPanel(ctx, parent, facetName);
                facet.getChildren().add(oldPanel);
                facet.getChildren().add(c);
                parent.getFacets().put(facetName, facet);
            }
        }
    }
    
    public static void removeFacet(FaceletContext ctx, UIComponent parent, UIComponent c, String facetName)
    {
        UIComponent facet = parent.getFacet(facetName);
        if (Boolean.TRUE.equals(facet.getAttributes().get(FACET_CREATED_UIPANEL_MARKER)))
        {
            facet.getChildren().remove(c);
        }
        else
        {
            parent.getFacets().remove(facetName);
        }
    }

    public static void markComponentToRestoreFully(FacesContext context, UIComponent component)
    {
        if (MyfacesConfig.getCurrentInstance(context.getExternalContext()).isRefreshTransientBuildOnPSSPreserveState())
        {
            component.getAttributes().put(DefaultFaceletsStateManagementStrategy.COMPONENT_ADDED_AFTER_BUILD_VIEW,
                                          ComponentState.REMOVE_ADD);
        }
        //component.subscribeToEvent(PostAddToViewEvent.class, new RestoreComponentFullyListener());
        if (FaceletViewDeclarationLanguage.isRefreshTransientBuildOnPSSAuto(context))
        {
            FaceletViewDeclarationLanguage.cleanTransientBuildOnRestore(context);
        }
    }
    
    public static UIComponent findComponentChildOrFacetFrom(FacesContext facesContext, UIComponent parent, String expr)
    {
        final char separatorChar = UINamingContainer.getSeparatorChar(facesContext);
        int separator = expr.indexOf(separatorChar);
        if (separator == -1)
        {
            return ComponentSupport.findComponentChildOrFacetFrom(
                    parent, expr, null);
        }
        else
        {
            return ComponentSupport.findComponentChildOrFacetFrom(
                    parent, expr.substring(0,separator), expr);
        }
    }
    
    public static UIComponent findComponentChildOrFacetFrom(UIComponent parent, String id, String innerExpr)
    {
        if (parent.getFacetCount() > 0)
        {
            for (UIComponent facet : parent.getFacets().values())
            {
                if (id.equals(facet.getId()))
                {
                    if (innerExpr == null)
                    {
                        return facet;
                    }
                    else if (facet instanceof NamingContainer)
                    {
                        UIComponent find = facet.findComponent(innerExpr);
                        if (find != null)
                        {
                            return find;
                        }
                    }
                }
                else if (!(facet instanceof NamingContainer))
                {
                    UIComponent find = findComponentChildOrFacetFrom(facet, id, innerExpr);
                    if (find != null)
                    {
                        return find;
                    }
                }
            }
        }
        if (parent.getChildCount() > 0)
        {
            for (int i = 0, childCount = parent.getChildCount(); i < childCount; i++)
            {
                UIComponent child = parent.getChildren().get(i);
                if (id.equals(child.getId()))
                {
                    if (innerExpr == null)
                    {
                        return child;
                    }
                    else if (child instanceof NamingContainer)
                    {
                        UIComponent find = child.findComponent(innerExpr);
                        if (find != null)
                        {
                            return find;
                        }
                    }
                }
                else if (!(child instanceof NamingContainer))
                {
                    UIComponent find = findComponentChildOrFacetFrom(child, id, innerExpr);
                    if (find != null)
                    {
                        return find;
                    }
                }
            }
        }
        return null;
    }
    
    public static String getFindComponentExpression(FacesContext facesContext, UIComponent component)
    {
        char separatorChar = UINamingContainer.getSeparatorChar(facesContext);
        UIComponent parent = component.getParent();
        StringBuilder sb = new StringBuilder();
        sb.append(component.getId());
        while (parent != null)
        {
            if (parent instanceof NamingContainer)
            {
                sb.insert(0, separatorChar);
                sb.insert(0, parent.getId());
            }
            parent = parent.getParent();
        }
        return sb.toString();
    }
    
    public static Object restoreInitialTagState(FaceletContext ctx, FaceletCompositionContext fcc,
                                                UIComponent parent, String uniqueId)
    {
        Object value = null;
        //if (fcc.isUsingPSSOnThisView() &&
        //    PhaseId.RESTORE_VIEW.equals(ctx.getFacesContext().getCurrentPhaseId()) &&
        //    !MyfacesConfig.getCurrentInstance(
        //            ctx.getFacesContext().getExternalContext()).isRefreshTransientBuildOnPSSPreserveState())
        if (fcc.isUsingPSSOnThisView() && !fcc.isRefreshTransientBuildOnPSSPreserveState())
        {
            UIViewRoot root = getViewRoot(ctx, parent);
            FaceletState map = (FaceletState) root.getAttributes().get(FACELET_STATE_INSTANCE);
            if (map == null)
            {
                value = null;
            }
            else
            {
                value = map.getState(uniqueId);
            }
        }
        return value;
    }
    
    public static void saveInitialTagState(FaceletContext ctx, FaceletCompositionContext fcc,
                                           UIComponent parent, String uniqueId, Object value)
    {
        // Only save the value when the view was built the first time, to ensure PSS algorithm 
        // work correctly. If preserve state is enabled, just ignore it, because this tag will
        // force full restore over the parent
        //if (fcc.isUsingPSSOnThisView()) {
        //    if (!fcc.isRefreshingTransientBuild() && !ctx.getFacesContext().isPostback()
        //        && !MyfacesConfig.getCurrentInstance(
        //            ctx.getFacesContext().getExternalContext()).isRefreshTransientBuildOnPSSPreserveState())
        
        // If we save the value each time the view is updated, we can use PSS on the dynamic parts,
        // just calling markInitialState() on the required components, simplifying the algorithm.
        if (fcc.isUsingPSSOnThisView() && !fcc.isRefreshTransientBuildOnPSSPreserveState())
        {
            UIViewRoot root = getViewRoot(ctx, parent);
            FaceletState map = (FaceletState) root.getAttributes().get(FACELET_STATE_INSTANCE);
            if (map == null)
            {
                map = new FaceletState();
                root.getAttributes().put(FACELET_STATE_INSTANCE, map);
            }
            map.putState(uniqueId, value);
        }
    }

}
