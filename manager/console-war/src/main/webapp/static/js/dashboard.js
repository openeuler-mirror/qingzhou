(function (global, $) {
    var DashboardManager = (function () {
        // 存储所有仪表板实例的对象
        var dashboards = {};

        function generateUniqueId() {
            return 'dashboard_' + Math.random().toString(36).substr(2, 9);
        }

        function cleanNotExistDashboard() {
            for (var key in dashboards) {
                var dashboard = dashboards[key];
                if (dashboard && dashboard.isHidden) {
                    continue;
                }
                dashboard = $('[data-dashboard-id="' + key + '"]');
                if (dashboard.length === 0) {
                    close(key);
                }
            }
        }

        function initializeDashboard(dashboardDiv) {
            cleanNotExistDashboard();
            if (!dashboardDiv || dashboardDiv.length === 0) {
                return null;
            }
            var uniqueId = dashboardDiv.attr('data-dashboard-id');
            var dashboard;
            var isNew = true;
            if (uniqueId) {
                dashboard = dashboards[uniqueId];
                if (dashboard) {
                    if (dashboard.isHidden) {
                        return dashboard.id;
                    }
                    isNew = false;
                }
            } else {
                uniqueId = generateUniqueId();
                dashboardDiv.attr('data-dashboard-id', uniqueId);
            }

            if (!dashboard) {
                dashboard = {
                    id: uniqueId,
                    div: dashboardDiv,
                    timeoutId: null,
                    isHidden: false,
                    chartInstances: {},
                    tableInstances: {},
                    shareDatasetBuffers: {}
                };
            }

            initDashboardPage(dashboard, isNew);

            return uniqueId;
        }

        function findDashboardId(container) {
            var dashboardDiv = $(container).find("div.dashboardPage");
            if (dashboardDiv.length > 0) {
                var dashboardId = dashboardDiv.attr("data-dashboard-id");
                if (dashboardId) {
                    return dashboardId;
                }
            }
            return null;
        }

        // 清理单个仪表板
        function cleanupDashboard(uniqueId) {
            var dashboard = dashboards[uniqueId];
            if (!dashboard) {
                return;
            }

            // 清除定时任务
            if (dashboard.timeoutId !== null) {
                clearTimeout(dashboard.timeoutId);
            }

            // 清理图表实例、表格实例和缓冲区
            disposeCacheChart(dashboard);
            dashboard.chartInstances = {};
            dashboard.tableInstances = {};
            dashboard.shareDatasetBuffers = {};

            // 移除 resize 事件监听器
            window.removeEventListener('resize', dashboard.resizeHandler);

            // 从 dashboards 对象中删除
            delete dashboards[uniqueId];
        }

        function hideDashboard(container) {
            var uniqueId = findDashboardId(container);
            if (!uniqueId) {
                return;
            }
            var dashboard = dashboards[uniqueId];
            if (!dashboard || dashboard.isHidden) {
                return;
            }

            dashboard.isHidden = true;

            if (dashboard.timeoutId !== null) {
                clearTimeout(dashboard.timeoutId);
                dashboard.timeoutId = null;
            }
        }

        function restoreDashboard(container) {
            var uniqueId = findDashboardId(container);
            if (!uniqueId) {
                return;
            }
            var dashboard = dashboards[uniqueId];
            if (!dashboard || !dashboard.isHidden) {
                return;
            }

            dashboard.isHidden = false;

            if (typeof dashboard.fetchDataAndRender === 'function') {
                dashboard.fetchDataAndRender();
                resizeHandler(dashboard);
            }
        }

        function close(uniqueId) {
            var dashboard = dashboards[uniqueId];
            if (!dashboard) {
                return;
            }

            cleanupDashboard(uniqueId);

            $(dashboard.div).remove();
        }

        function closeDashboard(container) {
            var uniqueId = findDashboardId(container);
            if (!uniqueId) {
                return;
            }
            close(uniqueId);
        }

        // 初始化仪表板页面的主要逻辑
        function initDashboardPage(dashboard, isNew) {
            cleanDashboard(dashboard);

            var dashboardDiv = dashboard.div;
            var url = dashboardDiv.attr("data-url");
            if (!url) {
                return;
            }

            if (isNew) {
                dashboards[dashboard.id] = dashboard;
            }

            // 绑定 resize 事件监听器
            dashboard.resizeHandler = function () {
                resizeHandler(dashboard);
            };
            window.addEventListener('resize', dashboard.resizeHandler);

            // 缓存容器选择器
            var containers = null;

            var failedAttempts = 0;
            var fetchDataAndRender = function () {
                fetchData(url).done(function (groupData, period) {
                    if (containers === null) {
                        containers = [];
                        for (var i in groupData) {
                            if (groupData.hasOwnProperty(i)) {
                                containers[i] = createGroupContainer(dashboardDiv, "dashboardData" + i);
                            }
                        }
                    }

                    for (var index in groupData) {
                        if (groupData.hasOwnProperty(index)) {
                            var container = containers[index];
                            var data = groupData[index];
                            for (var count in data) {
                                if (data.hasOwnProperty(count)) {
                                    try {
                                        var dashboardData = data[count];
                                        switch (dashboardData.type) {
                                            case getSetting("basicData"):
                                                renderBasicData(container, dashboardData);
                                                break;
                                            case getSetting("gaugeData"):
                                                renderGaugeData(container, dashboardData, count, dashboard);
                                                break;
                                            case getSetting("histogramData"):
                                                renderHistogramData(container, dashboardData, count, dashboard);
                                                break;
                                            case getSetting("shareDatasetData"):
                                                renderShareDatasetChart(container, dashboardData, count, dashboard);
                                                break;
                                            default:
                                                console.warn("无效的数据类型：" + dashboardData.type);
                                        }
                                    } catch (err) {
                                        console.error("渲染数据时出错:", err);
                                    }
                                }
                            }
                        }
                    }

                    // 重置失败次数
                    failedAttempts = 0;

                    var interval = 2000; // 默认间隔
                    if (typeof period === 'number' && period > 0) {
                        interval = period;
                    }
                    dashboard.timeoutId = setTimeout(fetchDataAndRender, interval);
                }).fail(function () {
                    failedAttempts++;
                    if (failedAttempts >= 10) {
                        console.error("数据获取失败，达到最大重试次数。");
                        return;
                    }
                    // 使用默认间隔重试，例如 2000ms
                    dashboard.timeoutId = setTimeout(fetchDataAndRender, 2000);
                });
            };

            // 保存 fetchDataAndRender 函数到 dashboard 对象中，以便恢复时调用
            dashboard.fetchDataAndRender = fetchDataAndRender;

            // 首次获取并渲染
            fetchDataAndRender();
        }

        // 清理仪表板状态
        function cleanDashboard(dashboard) {
            if (dashboard.timeoutId !== null) {
                clearTimeout(dashboard.timeoutId); // 停止定时任务
                disposeCacheChart(dashboard);
                dashboard.chartInstances = {};
                dashboard.tableInstances = {};
                dashboard.shareDatasetBuffers = {}; // 重置缓冲区
                window.removeEventListener('resize', dashboard.resizeHandler);
                dashboard.timeoutId = null;
            }
        }

        // 调整图表大小
        function resizeHandler(dashboard) {
            for (var chartId in dashboard.chartInstances) {
                if (dashboard.chartInstances.hasOwnProperty(chartId)) {
                    var chartInstance = dashboard.chartInstances[chartId];
                    if (typeof chartInstance.resize === 'function') {
                        setTimeout(function () {
                            chartInstance.resize();
                        }, 100); // 延迟 100ms
                    }
                }
            }
        }

        // 处置图表实例
        function disposeCacheChart(dashboard) {
            for (var chartId in dashboard.chartInstances) {
                if (dashboard.chartInstances.hasOwnProperty(chartId)) {
                    if (typeof dashboard.chartInstances[chartId].dispose === 'function') {
                        dashboard.chartInstances[chartId].dispose();
                    }
                }
            }
        }

        // 通过 AJAX 获取数据
        function fetchData(url) {
            var deferred = $.Deferred();

            $.ajax({
                type: "GET",
                url: url,
                dataType: 'json',
                success: function (data) {
                    deferred.resolve(data.data, data.period);
                },
                error: function (jqXHR, textStatus, errorThrown) {
                    console.error("数据请求失败:", textStatus, errorThrown);
                    deferred.reject();
                }
            });

            return deferred.promise();
        }

        // 创建分组容器
        function createGroupContainer(parentDiv, containerFlag) {
            var panel = $("<div></div>").addClass("panel").css({
                "border-radius": "2px",
                "border-color": "#EFEEEE",
                "background-color": "#FFFFFF"
            });
            var panelBody = $("<div></div>").addClass("panel-body").css({
                "word-break": "break-all",
                "padding": "0px"
            });
            var group = $("<div></div>").attr("container", containerFlag).css({
                "width": "100%"
            });
            parentDiv.append(panel);
            panel.append(panelBody);
            panelBody.append(group);

            return group;
        }

        // 渲染基本数据
        function renderBasicData(container, basicData) {
            var dataKey = getSetting("data");
            var jsonObj = basicData[dataKey];

            // 清空现有内容
            container.empty();

            // 渲染标题
            var titleText = basicData[getSetting("title")] || basicData[getSetting("info")];
            var title = createTitle(titleText);

            // 创建展示 basic 数据的容器
            var keyValueContainer = createChartContainer("98%");
            var itemDiv = $("<div></div>").addClass('basic-container');
            keyValueContainer.append(title, itemDiv);
            container.append(keyValueContainer);

            var keys = Object.keys(jsonObj);
            for (var i = 0; i < keys.length; i++) {
                var key = keys[i];
                var keyValueItem = $("<div></div>").addClass('basic-item');
                var keyElement = $("<div></div>").addClass('key').text(key);
                var valueElement = $("<div></div>").addClass('value').text(jsonObj[key]);

                keyValueItem.append(keyElement, valueElement);
                itemDiv.append(keyValueItem);
            }
        }

        // 渲染仪表盘数据
        function renderGaugeData(container, chartItem, index, dashboard) {
            var containerAttr = $(container).attr("container");
            var chartId = containerAttr + '_gauge_' + index;
            var data = chartItem[getSetting("data")];
            var fields = chartItem[getSetting("fields")];
            var max = parseFloat(chartItem[getSetting("max")]);
            var used = parseFloat(chartItem[getSetting("used")]);
            var info = chartItem[getSetting("info")] || "";
            var unit = chartItem[getSetting("unit")] || "";
            var titleText = chartItem[getSetting("title")] || info;

            // 检查是否已有图表实例
            var myChart = dashboard.chartInstances[chartId];
            var table = dashboard.tableInstances[chartId];

            if (!myChart) {
                // 创建图表
                var title = createTitle(titleText);
                var chartContainer = createChartContainer(getChartContainerWidth('gauge'));
                var chartDiv = $("<div></div>").addClass('chart').attr('id', chartId).css("height", "260px");
                chartContainer.append(title, chartDiv);
                container.append(chartContainer);

                myChart = echarts.init(chartDiv[0]);
                dashboard.chartInstances[chartId] = myChart;

                // 获取图表的配置并渲染
                var option = getGaugeOption({info: info, unit: unit, max: max, used: used});
                myChart.setOption(option);
            } else {
                // 更新图表数据和样式
                var option = getGaugeOption({info: info, unit: unit, max: max, used: used});
                myChart.setOption(option, true);
            }

            // 处理表格
            if (!table) {
                // 创建表格
                table = createTable(fields, data);
                if (table !== null) {
                    container.find("#" + chartId).parent().append(table);
                    dashboard.tableInstances[chartId] = table;
                }
            } else {
                // 更新表格
                updateTable(table, fields, data);
            }
        }

        // 渲染柱状图数据
        function renderHistogramData(container, chartItem, index, dashboard) {
            var containerAttr = $(container).attr("container");
            var chartId = containerAttr + '_bar_' + index;
            var data = chartItem[getSetting("data")];
            var fields = chartItem[getSetting("fields")];
            var max = chartItem[getSetting("max")] ? parseFloat(chartItem[getSetting("max")]) : undefined;
            var used = parseFloat(chartItem[getSetting("used")]);
            var info = chartItem[getSetting("info")] || "";
            var unit = chartItem[getSetting("unit")] || "";
            var titleText = chartItem[getSetting("title")] || info;

            // 检查是否已有图表实例
            var myChart = dashboard.chartInstances[chartId];
            var table = dashboard.tableInstances[chartId];

            if (!myChart) {
                var title = createTitle(titleText);
                var chartContainer = createChartContainer(getChartContainerWidth('bar'));
                var chartDiv = $("<div></div>").addClass('chart').attr('id', chartId).css("height", "300px");
                chartContainer.append(title, chartDiv);
                container.append(chartContainer);

                myChart = echarts.init(chartDiv[0]);
                dashboard.chartInstances[chartId] = myChart;

                // 获取图表的配置并渲染
                var option = getBarOption({info: info, unit: unit, max: max, used: used});
                myChart.setOption(option);
            } else {
                // 更新图表配置
                var newOption = getBarOption({info: info, unit: unit, max: max, used: used});
                myChart.setOption(newOption, true);
            }

            // 处理表格
            if (!table) {
                table = createTable(fields, data);
                if (table !== null) {
                    container.find("#" + chartId).parent().append(table);
                    dashboard.tableInstances[chartId] = table;
                }
            } else {
                updateTable(table, fields, data);
            }
        }

        // 渲染分享数据集图表
        function renderShareDatasetChart(container, chartItem, index, dashboard) {
            var containerAttr = $(container).attr("container");
            var pid = containerAttr + `_shareDataset_${index}`;
            var data = chartItem[getSetting("data")];
            var info = chartItem[getSetting("info")];
            var titleText = chartItem[getSetting("title")] || info;

            var myChart = dashboard.chartInstances[pid];
            if (!myChart) {
                // 初始化图表
                myChart = initShareDatasetChart(container, pid, titleText);
                dashboard.chartInstances[pid] = myChart;
                dashboard.shareDatasetBuffers[pid] = [];

                // 初始化 datasetSource
                var datasetSource = initShareDatasetSource(data);
                myChart.setOption(getShareDatasetChartOption(datasetSource, pid));

                // 绑定事件以控制缓冲
                bindShareDatasetChartEvents(myChart, pid, dashboard);
            } else {
                // 更新图表数据
                updateShareDatasetChartData(myChart, pid, data, dashboard);
            }
        }

        // 初始化分享数据集图表
        function initShareDatasetChart(container, pid, titleText) {
            // 创建标题和图表容器
            var title = createTitle(titleText);
            var chartContainer = createChartContainer("98%");
            var chartDiv = $("<div></div>").addClass('chart').attr('id', pid).css("height", "400px");

            chartContainer.append(title, chartDiv);
            $(container).append(chartContainer);

            // 初始化 ECharts 实例
            return echarts.init(chartDiv[0]);
        }

        function initShareDatasetSource(data) {
            var datasetSource = [
                ["dataTime", getDateTime()] // 初始化X轴
            ];
            data.forEach(item => datasetSource.push(item));
            return datasetSource;
        }

        // 获取分享数据集图表的配置
        function getShareDatasetChartOption(datasetSource, pid) {
            var series = datasetSource.slice(1).map(() => ({
                type: 'line',
                smooth: true,
                seriesLayoutBy: 'row',
                emphasis: {focus: 'series'},
                lineStyle: {width: 2},
                showSymbol: true,
                symbol: 'circle',
                symbolSize: 6
            }));

            var showDataIndex = datasetSource[0].length - 1;
            series.push({
                type: 'pie',
                id: pid,
                radius: '25%',
                center: ['50%', '25%'],
                emphasis: {focus: 'self'},
                label: {
                    show: true,
                    formatter: '{b}: {@' + showDataIndex + '} ({d}%)'
                },
                encode: {
                    itemName: datasetSource[0][0],
                    value: showDataIndex,
                    tooltip: showDataIndex
                }
            });

            return {
                legend: {},
                tooltip: {
                    trigger: 'axis',
                    showContent: true,
                    axisPointer: {
                        type: 'cross',
                        crossStyle: {
                            color: '#999'
                        }
                    },
                    formatter: function (params) {
                        var tooltipText = params[0].axisValue + '<br/>';
                        params.forEach(item => {
                            if (item.seriesType === 'line') {
                                tooltipText += `${item.marker} ${item.seriesName}: ${item.data[item.seriesIndex + 1]} <br/>`;
                            }
                        });
                        return tooltipText;
                    }
                },
                dataset: {
                    source: datasetSource
                },
                xAxis: {
                    type: 'category',
                    boundaryGap: false
                },
                yAxis: {
                    gridIndex: 0
                },
                grid: {top: '45%'},
                series: series,
                animation: true,
                animationDuration: 500
            };
        }

        // 绑定图表事件以控制缓冲区
        function bindShareDatasetChartEvents(myChart, pid, dashboard) {
            myChart.on('mouseover', params => {
                if (isShareDatasetChartLineSeries(params)) {
                    myChart.isPaused = true;
                }
            });

            myChart.on('mouseout', params => {
                if (isShareDatasetChartLineSeries(params)) {
                    myChart.isPaused = false;
                    applyShareDatasetBufferedData(pid, myChart, dashboard);
                }
            });

            myChart.on('updateAxisPointer', function (event) {
                var xAxisInfo = event.axesInfo[0];
                if (xAxisInfo) {
                    var dimension = xAxisInfo.value + 1;
                    myChart.setOption({
                        series: [{
                            id: pid,
                            label: {
                                formatter: '{b}: {@[' + dimension + ']} ({d}%)'
                            },
                            encode: {
                                value: dimension,
                                tooltip: dimension
                            }
                        }]
                    });
                }
            });
        }

        // 判断是否为折线图系列
        function isShareDatasetChartLineSeries(params) {
            return params.componentType === 'series' && (params.seriesType === 'line' || params.seriesType === 'pie');
        }

        // 更新分享数据集图表的数据
        function updateShareDatasetChartData(myChart, pid, data, dashboard) {
            if (myChart.isPaused) {
                // 添加新数据到缓冲区
                bufferShareDatasetData(dashboard, pid, data);
                return;
            }

            var chartData = myChart.getOption().dataset[0].source;
            var timestamp = getDateTime();
            chartData[0].push(timestamp);

            data.forEach((item, i) => {
                if (!chartData[i + 1]) {
                    chartData[i + 1] = [item[0]];
                }
                chartData[i + 1].push(item[1]);
            });

            // 保持缓冲区的数据量
            maintainDataLength(chartData);

            var latestIndex = chartData[0].length - 1;
            myChart.setOption({
                dataset: {source: chartData},
                series: [{
                    id: pid,
                    label: {
                        formatter: `{b}: {@${latestIndex}} ({d}%)`
                    },
                    encode: {
                        value: latestIndex,
                        tooltip: latestIndex
                    }
                }]
            });

            // 应用缓冲区的数据到图表
            applyShareDatasetBufferedData(myChart, pid, dashboard);
        }

        // 缓冲分享数据集数据
        function bufferShareDatasetData(dashboard, pid, data) {
            var buffer = dashboard.shareDatasetBuffers[pid];
            if (buffer.length >= 20) {
                buffer.shift();
            }
            buffer.push({
                timestamp: getDateTime(),
                data: data
            });
        }

        // 应用缓冲区的数据到图表
        function applyShareDatasetBufferedData(myChart, pid, dashboard) {
            var shareDatasetBuffers = dashboard.shareDatasetBuffers;
            var buffer = shareDatasetBuffers[pid];
            if (!buffer || buffer.length === 0) return;

            var chartData = myChart.getOption().dataset[0].source;

            buffer.forEach(entry => {
                chartData[0].push(entry.timestamp);
                entry.data.forEach((item, i) => {
                    if (!chartData[i + 1]) {
                        chartData[i + 1] = [item[0]];
                    }
                    chartData[i + 1].push(item[1]);
                });
                maintainDataLength(chartData);
            });

            shareDatasetBuffers[pid] = [];

            var latestIndex = chartData[0].length - 1;
            myChart.setOption({
                dataset: {source: chartData},
                series: [{
                    id: pid,
                    label: {
                        formatter: `{b}: {@${latestIndex}} ({d}%)`
                    },
                    encode: {
                        value: latestIndex,
                        tooltip: latestIndex
                    }
                }]
            });
        }

        // 保持数据长度
        function maintainDataLength(chartData) {
            if (chartData[0].length > 21) {
                chartData[0].splice(1, 1);
                chartData.slice(1).forEach(series => series.splice(1, 1));
            }
        }

        // 创建标题元素
        function createTitle(info) {
            return $("<div></div>").addClass('chart-title').text(info);
        }

        // 创建图表容器
        function createChartContainer(width) {
            return $("<div></div>").addClass('chart-container').css({
                "width": width,
                "margin": "10px"
            });
        }

        // 创建表格
        function createTable(fields, dataRows) {
            if (!fields || fields.length === 0 || !dataRows || dataRows.length <= 0) {
                return null;
            }
            var table = $("<table></table>").addClass('table-container');

            var thead = $("<thead></thead>");
            var headerRow = createTableTr();
            for (var i = 0; i < fields.length; i++) {
                headerRow.append($("<th></th>").text(fields[i]));
            }
            thead.append(headerRow);

            var tbody = $("<tbody></tbody>");
            for (var j = 0; j < dataRows.length; j++) {
                var row = dataRows[j];
                var dataRow = createTableTr();
                for (var k = 0; k < row.length; k++) {
                    dataRow.append($("<td></td>").text(row[k]));
                }
                tbody.append(dataRow);
            }

            table.append(thead).append(tbody);
            return table;
        }

        // 更新表格
        function updateTable(table, fields, dataRows) {
            if (!table || !fields || fields.length === 0 || !dataRows || dataRows.length <= 0) {
                return;
            }
            var thead = table.find("thead").empty();
            var headerRow = createTableTr();
            for (var i = 0; i < fields.length; i++) {
                headerRow.append($("<th></th>").text(fields[i]));
            }
            thead.append(headerRow);

            var tbody = table.find("tbody").empty();
            for (var j = 0; j < dataRows.length; j++) {
                var row = dataRows[j];
                var dataRow = createTableTr();
                for (var k = 0; k < row.length; k++) {
                    dataRow.append($("<td></td>").text(row[k]));
                }
                tbody.append(dataRow);
            }
        }

        // 创建表格行
        function createTableTr() {
            return $("<tr></tr>").css({
                "height": "25px"
            });
        }

        // 获取图表容器的宽度
        function getChartContainerWidth(chartType) {
            switch (chartType) {
                case 'gauge':
                case 'bar':
                    return "23%";
                default:
                    return "99%";
            }
        }

        // 获取仪表盘图表的配置
        function getGaugeOption({info, unit, max, used}) {
            var ratio = used / max;
            // 透明度：0.5:7f、0.55:8c、0.6:99、0.65:a5、0.7:b2、0.75:bf、0.8:cc、0.85:d8、0.9:e5、0.95:f2
            var chartTransparency = "a5";
            // 阈值
            var threshold = {"warn": 0.5, "alarm": 0.85, "full": 1};
            // 阈值对应颜色
            var colors = {"normal": "#9acd32", "warn": "#ffd700", "alarm": "#fd2100"};

            var color = colors.normal;
            var gradualColors = [{offset: 0, color: colors.normal + chartTransparency}];
            if (ratio < threshold.warn) {
                gradualColors.push({offset: 1, color: colors.normal});
            } else if (ratio >= threshold.warn && ratio <= threshold.alarm) {
                color = colors.warn;
                gradualColors.push({offset: threshold.warn, color: colors.normal});
                gradualColors.push({offset: threshold.full, color: colors.warn});
            } else if (ratio > threshold.alarm) {
                color = colors.alarm;
                gradualColors.push({offset: threshold.warn, color: colors.normal});
                gradualColors.push({offset: threshold.alarm, color: colors.warn});
                gradualColors.push({offset: threshold.full, color: colors.alarm});
            }

            return {
                tooltip: {
                    formatter: '{a} <br/>{b}: {c} ' + unit
                },
                series: [{
                    name: info,
                    type: 'gauge',
                    radius: '100%',
                    center: ['50%', '61%'], // 调整中心位置
                    min: 0,
                    max: max,
                    splitNumber: Math.min(10, max),
                    startAngle: 180,
                    endAngle: 0,
                    itemStyle: {
                        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, gradualColors),
                        shadowColor: '#008aff72',
                        shadowBlur: 10,
                        shadowOffsetX: 2,
                        shadowOffsetY: 2
                    },
                    progress: {
                        show: true,
                        roundCap: true,
                        width: 10
                    },
                    pointer: {
                        width: 8,
                        length: '65%',
                        offsetCenter: ['0', '5%'],
                        icon: 'path://M2090.36389,615.30999 L2090.36389,615.30999 C2091.48372,615.30999 2092.40383,616.194028 2092.44859,617.312956 L2096.90698,728.755929 C2097.05155,732.369577 2094.2393,735.416212 2090.62566,735.56078 C2090.53845,735.564269 2090.45117,735.566014 2090.36389,735.566014 L2090.36389,735.566014 C2086.74736,735.566014 2083.81557,732.63423 2083.81557,729.017692 C2083.81557,728.930412 2083.81732,728.84314 2083.82081,728.755929 L2088.2792,617.312956 C2088.32396,616.194028 2089.24407,615.30999 2090.36389,615.30999 Z',
                        itemStyle: {
                            color: color
                        }
                    },
                    axisLine: {
                        roundCap: true,
                        lineStyle: {
                            width: 10
                        }
                    },
                    axisTick: {splitNumber: 2, lineStyle: {width: 2, color: '#999'}},
                    splitLine: {length: 8, lineStyle: {width: 3, color: '#999'}},
                    axisLabel: {
                        distance: 18,
                        color: '#999',
                        fontSize: 14,
                        padding: [-2, -2, 0, -2],
                        formatter: function (v) {
                            return Number(v.toFixed(0));
                        }
                    },
                    title: {
                        show: true,
                        textStyle: {
                            fontSize: 14
                        },
                        formatter: info,
                        offsetCenter: [0, '51%']
                    },
                    detail: {
                        backgroundColor: 'rgba(0,0,0,0)',
                        borderWidth: 0,
                        borderColor: '#ccc',
                        width: 100,
                        lineHeight: 40,
                        height: 40,
                        borderRadius: 8,
                        offsetCenter: [0, '22%'],
                        valueAnimation: true,
                        rich: {
                            value: {fontSize: 20, fontWeight: 'bolder', color: '#777'},
                            unit: {fontSize: 20, color: '#999', padding: [0, 0, 0, 10]}
                        },
                        fontSize: 16,
                        formatter: '{value} ' + unit,
                        color: color
                    },
                    data: [
                        {
                            value: used,
                            name: info
                        }
                    ]
                }],
                backgroundColor: '#f9f9f9' // 设置背景色
            };
        }

        // 获取柱状图图表的配置
        function getBarOption(params) {
            var info = params.info;
            var unit = params.unit;
            var max = params.max;
            var used = params.used;
            var color = getColor(max, used); // 动态颜色

            var yAxisConfig = {
                type: 'value',
                axisLabel: {
                    formatter: '{value}'
                },
                axisLine: {
                    lineStyle: {color: '#333'}
                },
                axisTick: {
                    show: true,
                    lineStyle: {color: '#333'}
                },
                splitLine: {
                    lineStyle: {color: '#ccc'}
                }
            };

            if (max !== undefined && max !== 0) {
                yAxisConfig.max = max;
            }

            return {
                tooltip: {
                    trigger: 'axis',
                    axisPointer: {
                        type: 'shadow'
                    },
                    formatter: function (params) {
                        var tooltipText = params[0].name + '<br/>';
                        params.forEach(function (item) {
                            tooltipText += item.marker + item.axisValue + ': ' + item.value + ' ' + unit + '<br/>';
                        });
                        return tooltipText;
                    }
                },
                xAxis: {
                    type: 'category',
                    data: [info],
                    axisLine: {
                        lineStyle: {color: '#333'}
                    },
                    axisLabel: {
                        color: '#333',
                        fontSize: 14,
                        padding: [15, 0, 0, 0]
                    },
                    axisTick: {
                        alignWithLabel: true
                    }
                },
                yAxis: yAxisConfig,
                series: [{
                    type: 'bar',
                    data: [used],
                    label: {
                        show: true,
                        position: 'top',
                        formatter: '{c} ' + unit, // 在柱状图顶部显示值和单位
                        color: color,
                        fontSize: 14
                    },
                    itemStyle: {
                        color: color
                    }
                }],
                backgroundColor: '#f9f9f9', // 设置背景色
                grid: {
                    left: '10%',
                    right: '10%',
                    top: '10%',
                    bottom: '10%',
                    containLabel: true
                }
            };
        }

        // 获取柱状图的颜色
        function getColor(used, max) {
            if (max === undefined || max === 0) {
                return '#73c0de';
            }
            var ratio = used / max;
            if (ratio <= 0.5) {
                return '#73c0de'; // 蓝色
            } else if (ratio <= 0.8) {
                return '#f7ba2a'; // 黄色
            } else {
                return '#ef6666'; // 红色
            }
        }

        // 获取完整的 DashboardManager
        return {
            initialize: initializeDashboard,
            hide: hideDashboard,
            restore: restoreDashboard,
            close: closeDashboard
        };
    })();

    // 将 DashboardManager 暴露到全局作用域
    global.DashboardManager = DashboardManager;
})(window, jQuery);
