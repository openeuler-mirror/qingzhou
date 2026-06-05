package qingzhou.dto.meta.annotation;

import qingzhou.api.ChartType;
import qingzhou.api.FieldType;
import qingzhou.api.InputType;

public class ModelFieldView extends Base {
    public InputType input_type;

    public FieldType field_type;

    public String display;

    public String ref_model;

    public String link_to;

    public String default_value;

    public String[] options;

    public String separator;

    public boolean id;

    public boolean readonly;

    public boolean required;

    public long min;

    public long max;

    public int min_length;

    public int max_length;

    public boolean host;

    public boolean port;

    public boolean email;

    public boolean file;

    public String pattern;

    public boolean add;

    public boolean update;

    public boolean show;

    public boolean list;

    public boolean search;

    public boolean numeric;

    public ChartType chart_type;

    public String[] group;

    public String[] chart_group;

    public String[] color;

    public int width_percent;

    public String[] name;

    public String[] info;
}
