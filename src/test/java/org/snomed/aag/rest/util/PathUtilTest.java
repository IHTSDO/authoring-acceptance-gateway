package org.snomed.aag.rest.util;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PathUtilTest {
    @Test
    public void getParentPath_ShouldReturnNull_WhenGivenNull() {
        //when
        String result = PathUtil.getParentPath(null);

        //then
        assertNull(result);
    }

    @Test
    public void getParentPath_ShouldReturnNull_WhenGivenPathWithNoParent() {
        //when
        String result = PathUtil.getParentPath("PARENT");

        //then
        assertNull(result);
    }

    @Test
    public void getParentPath_ShouldReturnParent_WhenGivenPathWithParent() {
        //when
        String result = PathUtil.getParentPath("PARENT/CHILD");

        //then
        assertEquals("PARENT", result);
    }

    @Test
    public void getParentPath_ShouldReturnParent_WhenGivenPathWithGrandChild() {
        //when
        String result = PathUtil.getParentPath("PARENT/CHILD/GRANDCHILD");

        //then
        assertEquals("PARENT/CHILD", result);
    }
}