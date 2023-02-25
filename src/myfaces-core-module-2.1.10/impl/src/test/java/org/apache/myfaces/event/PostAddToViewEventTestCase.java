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
package org.apache.myfaces.event;

import javax.faces.application.StateManager;
import javax.faces.component.UICommand;
import javax.faces.component.UIForm;
import javax.faces.component.UIPanel;
import javax.faces.component.UIViewRoot;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.PhaseId;

import org.apache.myfaces.mc.test.core.AbstractMyFacesRequestTestCase;
import org.apache.myfaces.shared.util.WebConfigParamUtils;
import org.apache.myfaces.view.facelets.FaceletViewDeclarationLanguage;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Assert;
import org.junit.Test;

public class PostAddToViewEventTestCase extends AbstractMyFacesRequestTestCase
{
    
    
    @Override
    protected void setUpWebConfigParams() throws Exception
    {
        super.setUpWebConfigParams();
        servletContext.addInitParameter(StateManager.STATE_SAVING_METHOD_PARAM_NAME, StateManager.STATE_SAVING_METHOD_CLIENT);
        servletContext.addInitParameter("org.apache.myfaces.REFRESH_TRANSIENT_BUILD_ON_PSS", "true");
        servletContext.addInitParameter("javax.faces.PARTIAL_STATE_SAVING", "true");
    }

    /**
     * Test that UIViewRoot receive a PostAddToViewEvent after the view is
     * populated.
     * 
     * @throws Exception
     */
    @Test
    public void testPostAddToViewOnViewRoot() throws Exception
    {
        setupRequest("/postAddToViewEvent_1.xhtml");

        PostAddToViewEventBean bean = EasyMock.createMock(PostAddToViewEventBean.class);
        bean.invokePostAddToViewEvent(EasyMock.isA(ComponentSystemEvent.class));
        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>()
        {
            public Object answer()
            {
                ComponentSystemEvent e = (ComponentSystemEvent) EasyMock
                        .getCurrentArguments()[0];
                Assert.assertTrue(e.getComponent() instanceof UIViewRoot);
                Assert.assertEquals(PhaseId.RENDER_RESPONSE, facesContext.getCurrentPhaseId());
                return null;
            }
        }).once();
        EasyMock.replay(bean);
        //Put on request map
        request.setAttribute("postAddToViewEventBean", bean);
        processLifecycleExecuteAndRender();
        EasyMock.verify(bean);
        
        UICommand button = (UICommand) facesContext.getViewRoot().findComponent("mainForm:submit");
        submit(button);
        
        bean = EasyMock.createMock(PostAddToViewEventBean.class);
        bean.invokePostAddToViewEvent(EasyMock.isA(ComponentSystemEvent.class));
        // With PSS PostAddToViewEvent is called again on restore view phase
        // but when building initial state.
        if (WebConfigParamUtils.getBooleanInitParameter(externalContext, StateManager.PARTIAL_STATE_SAVING_PARAM_NAME))
        {
            EasyMock.expectLastCall().andAnswer(new IAnswer<Object>()
            {
                public Object answer()
                {
                    ComponentSystemEvent e = (ComponentSystemEvent) EasyMock
                            .getCurrentArguments()[0];
                    Assert.assertTrue(e.getComponent() instanceof UIViewRoot);
                    Assert.assertEquals(PhaseId.RESTORE_VIEW, facesContext.getCurrentPhaseId());
                    Assert.assertTrue(facesContext.getAttributes().containsKey("javax.faces.IS_BUILDING_INITIAL_STATE"));
                    Assert.assertFalse(FaceletViewDeclarationLanguage.isRefreshingTransientBuild(facesContext));
                    return null;
                }
            }).once();
        }
        else
        {
            // Without PSS the listener should not be called, because it was already
            // called on the first request
            EasyMock.expectLastCall().andAnswer(new IAnswer<Object>()
            {
                public Object answer()
                {
                    Assert.fail();
                    return null;
                }
            }).anyTimes();
        }
        
        EasyMock.replay(bean);
        //Put on request map
        request.setAttribute("postAddToViewEventBean", bean);
        processLifecycleExecute();
        EasyMock.verify(bean);
    }

    public static interface PostAddToViewEventBean
    {
        public void invokePostAddToViewEvent(ComponentSystemEvent e);
    }
    
    /**
     * Test a component receive PostAddToViewEvent
     * 
     * @throws Exception
     */
    @Test
    public void testPostAddToViewOnComponent() throws Exception
    {
        setupRequest("/postAddToViewEvent_2.xhtml");

        PostAddToViewEventBean bean = EasyMock.createMock(PostAddToViewEventBean.class);
        bean.invokePostAddToViewEvent(EasyMock.isA(ComponentSystemEvent.class));
        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>()
        {
            public Object answer()
            {
                ComponentSystemEvent e = (ComponentSystemEvent) EasyMock
                        .getCurrentArguments()[0];
                Assert.assertTrue(e.getComponent() instanceof UIForm);
                Assert.assertEquals(PhaseId.RENDER_RESPONSE, facesContext.getCurrentPhaseId());
                return null;
            }
        }).once();
        EasyMock.replay(bean);
        //Put on request map
        request.setAttribute("postAddToViewEventBean", bean);
        processLifecycleExecuteAndRender();
        EasyMock.verify(bean);
        
        UICommand button = (UICommand) facesContext.getViewRoot().findComponent("mainForm:submit");
        submit(button);
        
        bean = EasyMock.createMock(PostAddToViewEventBean.class);
        bean.invokePostAddToViewEvent(EasyMock.isA(ComponentSystemEvent.class));
        // With PSS PostAddToViewEvent is called again on restore view phase
        // but when building initial state.
        if (WebConfigParamUtils.getBooleanInitParameter(externalContext, StateManager.PARTIAL_STATE_SAVING_PARAM_NAME))
        {
            EasyMock.expectLastCall().andAnswer(new IAnswer<Object>()
            {
                public Object answer()
                {
                    ComponentSystemEvent e = (ComponentSystemEvent) EasyMock
                            .getCurrentArguments()[0];
                    Assert.assertTrue(e.getComponent() instanceof UIForm);
                    Assert.assertEquals(PhaseId.RESTORE_VIEW, facesContext.getCurrentPhaseId());
                    Assert.assertTrue(facesContext.getAttributes().containsKey("javax.faces.IS_BUILDING_INITIAL_STATE"));
                    Assert.assertFalse(FaceletViewDeclarationLanguage.isRefreshingTransientBuild(facesContext));
                    return null;
                }
            }).once();
        }
        else
        {
            // Without PSS the listener should not be called, because it was already
            // called on the first request
            EasyMock.expectLastCall().andAnswer(new IAnswer<Object>()
            {
                public Object answer()
                {
                    Assert.fail();
                    return null;
                }
            }).anyTimes();
        }
        
        EasyMock.replay(bean);
        //Put on request map
        request.setAttribute("postAddToViewEventBean", bean);
        processLifecycleExecute();
        EasyMock.verify(bean);
    }

    /**
     * Test that a component added dynamically should receive PostAddToViewEvent
     * 
     * @throws Exception
     */
    @Test
    public void testPostAddToViewOnComponentCif() throws Exception
    {
        setupRequest("/postAddToViewEvent_3.xhtml");

        PostAddToViewEventBean bean = EasyMock.createMock(PostAddToViewEventBean.class);
        bean.invokePostAddToViewEvent(EasyMock.isA(ComponentSystemEvent.class));
        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>()
        {
            public Object answer()
            {
                Assert.fail();
                return null;
            }
        }).anyTimes();
        EasyMock.replay(bean);
        //Put on request map
        request.setAttribute("postAddToViewEventBean", bean);
        request.setAttribute("condition", Boolean.FALSE);
        processLifecycleExecuteAndRender();
        EasyMock.verify(bean);
        
        UICommand button = (UICommand) facesContext.getViewRoot().findComponent("mainForm:submit");
        submit(button);
        
        bean = EasyMock.createMock(PostAddToViewEventBean.class);
        bean.invokePostAddToViewEvent(EasyMock.isA(ComponentSystemEvent.class));
        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>()
        {
            public Object answer()
            {
                Assert.fail();
                return null;
            }
        }).anyTimes();
        
        EasyMock.replay(bean);
        //Put on request map
        request.setAttribute("postAddToViewEventBean", bean);
        processLifecycleExecute();
        EasyMock.verify(bean);
        
        bean = EasyMock.createMock(PostAddToViewEventBean.class);
        bean.invokePostAddToViewEvent(EasyMock.isA(ComponentSystemEvent.class));
        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>()
        {
            public Object answer()
            {
                ComponentSystemEvent e = (ComponentSystemEvent) EasyMock
                        .getCurrentArguments()[0];
                Assert.assertTrue(e.getComponent() instanceof UIPanel);
                Assert.assertEquals(PhaseId.RENDER_RESPONSE, facesContext.getCurrentPhaseId());
                return null;
            }
        }).once();
        EasyMock.replay(bean);
        
        request.setAttribute("postAddToViewEventBean", bean);
        request.setAttribute("condition", Boolean.TRUE);
        processRender();
        EasyMock.verify(bean);
    }
}
