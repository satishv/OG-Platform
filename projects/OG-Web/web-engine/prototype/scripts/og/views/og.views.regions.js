/*
 * @copyright 2009 - present by OpenGamma Inc
 * @license See distribution for license
 */
$.register_module({
    name: 'og.views.regions',
    dependencies: [
        'og.api.rest',
        'og.api.text',
        'og.common.masthead.menu',
        'og.common.routes',
        'og.common.search_results.core',
        'og.common.util.history',
        'og.common.util.ui.dialog',
        'og.common.util.ui.message',
        'og.common.util.ui.toolbar',
        'og.views.common.layout',
        'og.views.common.state',
        'og.views.common.default_details'
    ],
    obj: function () {
        var api = og.api.rest, routes = og.common.routes, module = this, view,
            masthead = og.common.masthead, search, layout, details = og.common.details,
            ui = og.common.util.ui, history = og.common.util.history,
            page_name = module.name.split('.').pop(),
            check_state = og.views.common.state.check.partial('/' + page_name),
            options = {
                toolbar: {
                    active: {
                        buttons: [
                            {id: 'new', tooltip: 'New', enabled: 'OG-disabled'},
                            {id: 'save', tooltip: 'Save', enabled: 'OG-disabled'},
                            {id: 'saveas', tooltip: 'Save as', enabled: 'OG-disabled'},
                            {id: 'delete', tooltip: 'Delete', enabled: 'OG-disabled'}
                        ],
                        location: '.OG-tools'
                    },
                    'default': {
                        buttons: [
                            {id: 'new', tooltip: 'New', enabled: 'OG-disabled'},
                            {id: 'save', tooltip: 'Save', enabled: 'OG-disabled'},
                            {id: 'saveas', tooltip: 'Save as', enabled: 'OG-disabled'},
                            {id: 'delete', tooltip: 'Delete', enabled: 'OG-disabled'}
                        ],
                        location: '.OG-tools'
                    }
                },
                slickgrid: {
                    'selector': '.OG-js-search', 'page_type': page_name,
                    'columns': [
                        {
                            id: 'name', field: 'name',width: 300, cssClass: 'og-link', toolTip: 'name',
                            name: '<input type="text" placeholder="Name" class="og-js-name-filter" ' +
                                'style="width: 280px;">'
                        }
                    ]
                }
            },
            default_details = og.views.common.default_details.partial(page_name, 'Regions', options);
        module.rules = {
            load: {route: '/' + page_name + '/name:?', method: module.name + '.load'},
            load_filter: {route: '/' + page_name + '/filter:/:id?/name:?', method: module.name + '.load_filter'},
            load_item: {route: '/' + page_name + '/:id/:node?/name:?', method: module.name + '.load_item'}
        };
        return view = {
            details: function (args) {
                layout.inner.options.south.onclose = null;
                layout.inner.close('south');
                api.regions.get({
                    dependencies: ['id'],
                    handler: function (result) {
                        if (result.error) return alert(result.message);
                        var f = details.region_functions;
                        var json = result.data;
                        history.put({
                            name: json.template_data.name,
                            item: 'history.' + page_name + '.recent',
                            value: routes.current().hash
                        });
                        og.api.text({module: module.name, handler: function (template) {
                            var $html = $.tmpl(template, json);
                            $('.ui-layout-inner-center .ui-layout-header').html($html.find('> header'));
                            $('.ui-layout-inner-center .ui-layout-content').html($html.find('> section'));
                            layout.inner.close('north'), $('.ui-layout-inner-north').empty();
                            f.render_regions('.OG-details-content .og-js-parent_regions', json.parent);
                            f.render_regions('.OG-details-content .og-js-child_regions', json.child);
                            ui.message({location: '.ui-layout-inner-center', destroy: true});
                            ui.toolbar(options.toolbar.active);
                            setTimeout(layout.inner.resizeAll);
                        }});
                    },
                    id: args.id,
                    loading: function () {
                        ui.message({
                            location: '.ui-layout-inner-center',
                            message: {0: 'loading...', 3000: 'still loading...'}
                        });
                    },
                    update: view.details.partial(args)
                });
            },
            filters: ['name'],
            load: function (args) {
                layout = og.views.common.layout;
                check_state({args: args, conditions: [
                    {new_page: function (args) {view.search(args), masthead.menu.set_tab(page_name);}}
                ]});
                if (!args.id) default_details();
            },
            load_filter: function (args) {
                check_state({args: args, conditions: [{new_value: 'id', method: function (args) {
                    view[args.id ? 'load_item' : 'load'](args);
                }}]});
                search.filter(args);
            },
            load_item: function (args) {
                check_state({args: args, conditions: [{new_page: view.load}]});
                view.details(args);
            },
            search: function (args) {
                if (!search) search = og.common.search_results.core();
                search.load($.extend(options.slickgrid, {url: args}));
            },
            init: function () {for (var rule in module.rules) routes.add(module.rules[rule]);},
            rules: module.rules
        };
    }
});
