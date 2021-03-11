package com.epam.deltix.tbwg.webapp.model.charting.line;


import java.util.Objects;

/**
 * A line point that defines a tag.
 * @label Tag
 */
public abstract class TagElementDef extends LineElementDef {
    private TagType tagType;
    private String value;

    /**
     * Tag type.
     */
    public TagType getTagType() {
        return tagType;
    }

    public void setTagType(TagType tagType) {
        this.tagType = tagType;
    }

    /**
     * Tag value. An Y coordinate where the tag should be drawn.
     */
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TagElementDef that = (TagElementDef) o;
        return tagType == that.tagType &&
            Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), tagType, value);
    }
}
